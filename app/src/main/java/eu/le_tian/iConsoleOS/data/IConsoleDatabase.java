package eu.le_tian.iConsoleOS.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {User.class, Settings.class, Exercise.class, ExerciseData.class, ExerciseProfile.class, ExerciseProfileData.class, ExerciseSamples.class}, version = IConsoleDatabase.VERSION)
public abstract class IConsoleDatabase extends RoomDatabase {
    /** Bump together with a new Migration and the exported schema in app/schemas. */
    public static final int VERSION = 8;

    public abstract UserDao userDao();

    public abstract SettingsDao settingsDao();

    public abstract ExerciseDAO exerciseDAO();

    public abstract ExerciseDataDao exerciseDataDao();

    public abstract ExerciseProfileDao exerciseProfileDao();

    public abstract ExerciseProfileDataDao exerciseProfileDataDao();

    public abstract ExerciseSamplesDao exerciseSamplesDao();

    private static String TAG = "IConsoleDB";

    private static volatile IConsoleDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Exercise ADD COLUMN summarized INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE Exercise ADD COLUMN totalDuration INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE Exercise ADD COLUMN totalDistance INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE Exercise ADD COLUMN totalCalories INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE Exercise ADD COLUMN avgPower INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE Exercise ADD COLUMN maxPower INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE Exercise ADD COLUMN avgSpeed INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE Exercise ADD COLUMN maxSpeed INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE Exercise ADD COLUMN avgHR INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE Exercise ADD COLUMN maxHR INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE Exercise ADD COLUMN avgRPM INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE Exercise ADD COLUMN maxRPM INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Must match the index Room derives from @Index("exerciseParentID") on ExerciseData
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ExerciseData_exerciseParentID` ON `ExerciseData` (`exerciseParentID`)");
        }
    };

    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Schema only: existing rows are moved by the "Compact data" action, not here,
            // so the upgrade is instant and can't fail halfway through the user's data.
            database.execSQL("CREATE TABLE IF NOT EXISTS `ExerciseSamples` (`exerciseID` INTEGER NOT NULL, `sampleCount` INTEGER NOT NULL, `codecVersion` INTEGER NOT NULL, `data` BLOB NOT NULL, PRIMARY KEY(`exerciseID`))");
        }
    };

    static IConsoleDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (IConsoleDatabase.class) {
                if (INSTANCE == null) {
                    Log.v(TAG, "init db");
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), IConsoleDatabase.class, "IConsoleDB")
                            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                            .addCallback(rdbc)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Closes the database so its file can be replaced by a restore. The app must restart right
     * after: repositories created earlier still hold the closed instance.
     */
    static void closeForRestore() {
        synchronized (IConsoleDatabase.class) {
            if (INSTANCE != null) {
                INSTANCE.close();
                INSTANCE = null;
            }
        }
    }

    private static void clearDB() {

    }

    private static void initDb() {
        databaseWriteExecutor.execute(() -> {
            //instance of all different DAOs:
            SettingsDao settingsDao = INSTANCE.settingsDao();
            UserDao userDao = INSTANCE.userDao();
            ExerciseDAO exerciseDAO = INSTANCE.exerciseDAO();
            ExerciseDataDao exerciseDataDao = INSTANCE.exerciseDataDao();
            ExerciseProfileDao exerciseProfileDao = INSTANCE.exerciseProfileDao();
            ExerciseProfileDataDao exerciseProfileDataDao = INSTANCE.exerciseProfileDataDao();
            Log.d(TAG, "emptying db");
            settingsDao.deleteAll();
            userDao.deleteAll();
            exerciseDAO.deleteAll();
            exerciseDataDao.deleteAll();
            exerciseProfileDao.deleteAll();
            exerciseProfileDataDao.deleteAll();
            Log.d(TAG, "writing initial values");
            User user = new User(0, "Default");
            userDao.insert(user);
            Settings settings = new Settings(0, 1, 1, 5);
            settingsDao.insert(settings);

            long exerciseProfileID;

            ExerciseProfile exerciseProfile = new ExerciseProfile(0, 1, "Quick Start", 1, 0, true);
            exerciseProfileID = exerciseProfileDao.insert(exerciseProfile);


            exerciseProfile = new ExerciseProfile(0, 2, "Interval training", 1, 2500, true);
            exerciseProfileID = exerciseProfileDao.insert(exerciseProfile);

            ExerciseProfileData exerciseProfileData = new ExerciseProfileData(0, 1, 0, 5, 120, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);
            exerciseProfileData = new ExerciseProfileData(0, 2, 1, 8, 300, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);
            exerciseProfileData = new ExerciseProfileData(0, 3, 1, 28, 40, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);
            exerciseProfileData = new ExerciseProfileData(0, 4, 2, 4, 120, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);


            exerciseProfile = new ExerciseProfile(0, 3, "Interval Extended", 1, 2500, false);
            exerciseProfileID = exerciseProfileDao.insert(exerciseProfile);

            exerciseProfileData = new ExerciseProfileData(0, 1, 0, 5, 120, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);
            exerciseProfileData = new ExerciseProfileData(0, 2, 1, 8, 300, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);
            exerciseProfileData = new ExerciseProfileData(0, 3, 1, 28, 40, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);
            exerciseProfileData = new ExerciseProfileData(0, 4, 2, 4, 120, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);

            exerciseProfile = new ExerciseProfile(0, 4, "Interval 2", 1, 2500, false);
            exerciseProfileID = exerciseProfileDao.insert(exerciseProfile);

            exerciseProfileData = new ExerciseProfileData(0, 1, 0, 5, 120, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);
            exerciseProfileData = new ExerciseProfileData(0, 2, 1, 8, 300, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);
            exerciseProfileData = new ExerciseProfileData(0, 2, 1, 14, 120, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);
            exerciseProfileData = new ExerciseProfileData(0, 3, 1, 28, 40, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);
            exerciseProfileData = new ExerciseProfileData(0, 4, 2, 4, 120, exerciseProfileID);
            exerciseProfileDataDao.insert(exerciseProfileData);
        });
    }

    private static Callback rdbc = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            Log.d(TAG, "creating db in callback onCreate");

            //databaseWriteExecutor.execute(() -> {
            //    Log.d(TAG,"writing to instance onCreate");

            //});
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            Log.d(TAG, "in callback onOpen");
            //initDb();
        }

    };
}
