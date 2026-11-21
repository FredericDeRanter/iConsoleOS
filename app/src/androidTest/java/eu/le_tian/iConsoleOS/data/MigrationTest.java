package eu.le_tian.iConsoleOS.data;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Upgrades from every older schema version to the current one and checks that no workout data
 * is lost. Uses its own database name, never the app's "IConsoleDB".
 *
 * <p>Run it on an EMULATOR, not on the tablet: Gradle's connectedAndroidTest uninstalls the app
 * afterwards, which deletes the real workout history. See AGENTS.md.
 *
 * <p>5.json was reconstructed (v5 predates schema export) from 6.json minus the summary columns
 * and checked column by column against a real v5 database; its identity hash is the real one.
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTest {
    private static final String TEST_DB = "migration-test";
    private static final Migration[] ALL = {
            IConsoleDatabase.MIGRATION_5_6, IConsoleDatabase.MIGRATION_6_7, IConsoleDatabase.MIGRATION_7_8,
            IConsoleDatabase.MIGRATION_8_9};

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), IConsoleDatabase.class);

    @After
    public void deleteTestDb() {
        context().deleteDatabase(TEST_DB);
    }

    private static Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /** Rows every version has: a user, settings, a profile with steps, two workouts with samples. */
    private static void insertCommonRows(SupportSQLiteDatabase db, int version) {
        db.execSQL("INSERT INTO User (userID, userName) VALUES (1, 'Default')");
        db.execSQL("INSERT INTO Settings (settingsID, userParentID, defaultProfileID, quickIncrease) VALUES (1, 1, 1, 5)");
        db.execSQL("INSERT INTO ExerciseProfile (exerciseProfileID, sortOrder, exerciseProfileName, startLevel, defaultDuration, repeatToFill) VALUES (2, 2, 'Interval training', 1, 2500, 1)");
        db.execSQL("INSERT INTO ExerciseProfileData (exerciseProfileDataID, sortOrder, dataType, relativeLevel, duration, parentExerciseProfileID) VALUES (1, 1, 0, 5, 120, 2)");
        db.execSQL("INSERT INTO ExerciseProfileData (exerciseProfileDataID, sortOrder, dataType, relativeLevel, duration, parentExerciseProfileID) VALUES (2, 2, 1, 8, 300, 2)");
        insertExercise(db, version, 10, 1595876362000L, 1595879160000L, "Interval training");
        insertExercise(db, version, 11, 1700000000000L, 0, "Quick Start");
        for (int t = 0; t < 50; t++) {
            db.execSQL("INSERT INTO ExerciseData (exPower, exSpeed, exDistance, exCalories, exHR, exRPM, exTimestamp, exerciseParentID) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{1000 + t, 250, t / 5, t / 10, 120, 70, t / 2, 10});
        }
        db.execSQL("INSERT INTO ExerciseData (exPower, exSpeed, exDistance, exCalories, exHR, exRPM, exTimestamp, exerciseParentID) VALUES (900, 200, 0, 0, 100, 60, 0, 11)");
    }

    /**
     * From v6 on the summary columns are NOT NULL without a default (Room writes every column),
     * so they must be given; v5 has no summary columns.
     */
    private static void insertExercise(SupportSQLiteDatabase db, int version, long id, long start, long stop, String profile) {
        if (version < 6) {
            db.execSQL("INSERT INTO Exercise (exerciseID, exStartDateTime, exStopDateTime, exProfileName, userParentID) VALUES (?, ?, ?, ?, 1)",
                    new Object[]{id, start, stop, profile});
        } else {
            db.execSQL("INSERT INTO Exercise (exerciseID, exStartDateTime, exStopDateTime, exProfileName, userParentID, "
                            + "summarized, totalDuration, totalDistance, totalCalories, avgPower, maxPower, avgSpeed, maxSpeed, avgHR, maxHR, avgRPM, maxRPM) "
                            + "VALUES (?, ?, ?, ?, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)",
                    new Object[]{id, start, stop, profile});
        }
    }

    private static long count(SupportSQLiteDatabase db, String table) {
        try (Cursor c = db.query("SELECT COUNT(*) FROM " + table)) {
            c.moveToFirst();
            return c.getLong(0);
        }
    }

    private static void assertCommonRowsIntact(SupportSQLiteDatabase db) {
        assertEquals(1, count(db, "User"));
        assertEquals(1, count(db, "Settings"));
        assertEquals(1, count(db, "ExerciseProfile"));
        assertEquals(2, count(db, "ExerciseProfileData"));
        assertEquals(2, count(db, "Exercise"));
        assertEquals(51, count(db, "ExerciseData"));
        try (Cursor c = db.query("SELECT exStartDateTime, exStopDateTime, exProfileName FROM Exercise WHERE exerciseID = 10")) {
            assertTrue(c.moveToFirst());
            assertEquals(1595876362000L, c.getLong(0));
            assertEquals(1595879160000L, c.getLong(1));
            assertEquals("Interval training", c.getString(2));
        }
        try (Cursor c = db.query("SELECT SUM(exPower), MAX(exTimestamp) FROM ExerciseData WHERE exerciseParentID = 10")) {
            assertTrue(c.moveToFirst());
            assertEquals(50 * 1000 + 49 * 50 / 2, c.getLong(0));
            assertEquals(24, c.getLong(1));
        }
    }

    private static void assertCurrentOnlyStructures(SupportSQLiteDatabase db) {
        try (Cursor c = db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_ExerciseData_exerciseParentID'")) {
            assertTrue("v7 index missing", c.moveToFirst());
        }
        assertEquals(0, count(db, "ExerciseSamples"));
        // v9 ride metrics start unknown.
        try (Cursor c = db.query("SELECT normPower, best5min, best20min, best60min, metricsVersion FROM Exercise WHERE exerciseID = 10")) {
            assertTrue(c.moveToFirst());
            for (int i = 0; i < 5; i++)
                assertEquals(0, c.getInt(i));
        }
    }

    @Test
    public void migrate5ToCurrentKeepsData() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 5)) {
            insertCommonRows(db, 5);
        }
        SupportSQLiteDatabase db = helper.runMigrationsAndValidate(TEST_DB, IConsoleDatabase.VERSION, true, ALL);
        assertCommonRowsIntact(db);
        assertCurrentOnlyStructures(db);
        // Summary columns added in v6 start empty and unsummarized.
        try (Cursor c = db.query("SELECT summarized, totalDuration, avgPower FROM Exercise WHERE exerciseID = 10")) {
            assertTrue(c.moveToFirst());
            assertEquals(0, c.getInt(0));
            assertEquals(0, c.getLong(1));
            assertEquals(0, c.getInt(2));
        }
    }

    @Test
    public void migrate6ToCurrentKeepsSummaries() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 6)) {
            insertCommonRows(db, 6);
            db.execSQL("UPDATE Exercise SET summarized = 1, totalDuration = 2798, avgPower = 1069, maxPower = 1388, avgHR = 11 WHERE exerciseID = 10");
        }
        SupportSQLiteDatabase db = helper.runMigrationsAndValidate(TEST_DB, IConsoleDatabase.VERSION, true,
                IConsoleDatabase.MIGRATION_6_7, IConsoleDatabase.MIGRATION_7_8, IConsoleDatabase.MIGRATION_8_9);
        assertCommonRowsIntact(db);
        assertCurrentOnlyStructures(db);
        try (Cursor c = db.query("SELECT summarized, totalDuration, avgPower, maxPower, avgHR FROM Exercise WHERE exerciseID = 10")) {
            assertTrue(c.moveToFirst());
            assertEquals(1, c.getInt(0));
            assertEquals(2798, c.getLong(1));
            assertEquals(1069, c.getInt(2));
            assertEquals(1388, c.getInt(3));
            assertEquals(11, c.getInt(4));
        }
    }

    @Test
    public void migrate7ToCurrentKeepsData() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 7)) {
            insertCommonRows(db, 7);
        }
        SupportSQLiteDatabase db = helper.runMigrationsAndValidate(TEST_DB, IConsoleDatabase.VERSION, true,
                IConsoleDatabase.MIGRATION_7_8, IConsoleDatabase.MIGRATION_8_9);
        assertCommonRowsIntact(db);
        assertCurrentOnlyStructures(db);
    }

    @Test
    public void migrate8ToCurrentKeepsSummariesAndBlobs() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 8)) {
            insertCommonRows(db, 8);
            db.execSQL("UPDATE Exercise SET summarized = 1, totalDuration = 2555, avgPower = 1131, maxPower = 1221 WHERE exerciseID = 11");
            db.execSQL("INSERT INTO ExerciseSamples (exerciseID, sampleCount, codecVersion, data) VALUES (11, 3, 1, X'0102030405')");
        }
        SupportSQLiteDatabase db = helper.runMigrationsAndValidate(TEST_DB, IConsoleDatabase.VERSION, true,
                IConsoleDatabase.MIGRATION_8_9);
        assertEquals(2, count(db, "Exercise"));
        assertEquals(51, count(db, "ExerciseData"));
        try (Cursor c = db.query("SELECT summarized, totalDuration, avgPower, maxPower, normPower, metricsVersion FROM Exercise WHERE exerciseID = 11")) {
            assertTrue(c.moveToFirst());
            assertEquals(1, c.getInt(0));
            assertEquals(2555, c.getLong(1));
            assertEquals(1131, c.getInt(2));
            assertEquals(1221, c.getInt(3));
            assertEquals(0, c.getInt(4));
            assertEquals(0, c.getInt(5));
        }
        try (Cursor c = db.query("SELECT sampleCount, hex(data) FROM ExerciseSamples WHERE exerciseID = 11")) {
            assertTrue(c.moveToFirst());
            assertEquals(3, c.getInt(0));
            assertEquals("0102030405", c.getString(1));
        }
    }

    /** After migrating, the real Room database class opens it and the DAOs read the old data. */
    @Test
    public void roomOpensMigratedDatabase() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 5)) {
            insertCommonRows(db, 5);
        }
        IConsoleDatabase room = Room.databaseBuilder(context(), IConsoleDatabase.class, TEST_DB)
                .addMigrations(ALL)
                .build();
        helper.closeWhenFinished(room);

        List<ExerciseData> samples = room.exerciseDataDao().getExerciseDataNotLive(10);
        assertEquals(50, samples.size());
        assertEquals(1000, samples.get(0).getExPower());   // ordered by insertion
        assertEquals(1049, samples.get(49).getExPower());

        Exercise ex = room.exerciseDAO().getExerciseByIDNotLive(11);
        assertNotNull(ex);
        assertFalse(ex.isSummarized());
        assertEquals(1, room.exerciseProfileDao().getExerciseProfileNoLive().size());
    }
}
