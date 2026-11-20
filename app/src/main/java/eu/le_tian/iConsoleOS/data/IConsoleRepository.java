package eu.le_tian.iConsoleOS.data;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class IConsoleRepository {
    private static String TAG = "Repository";
    /**
     * ID of the workout being recorded right now, or -1. Process-wide because every fragment has
     * its own repository instance; a crash resets it, so a crashed workout gets compacted later.
     */
    private static volatile long sRecordingExerciseID = -1;
    private ExerciseProfileDao exerciseProfileDao;
    private ExerciseProfileDataDao exerciseProfileDataDao;
    private SettingsDao settingsDao;
    private UserDao userDao;
    private ExerciseDAO exerciseDAO;
    private ExerciseDataDao exerciseDataDao;
    private ExerciseSamplesDao exerciseSamplesDao;
    public IConsoleDatabase db;

    private LiveData<List<ExerciseProfile>> mAllExerciseProfiles;
    private LiveData<List<ExerciseProfileData>> mAllExerciseProfileData;
    private LiveData<List<ExerciseProfileData>> mExerciseProfileDataForID;
    private Long selectedExerciseProfileID;

    public IConsoleRepository(Application application) {
        Log.v(TAG, "init db in repo");
        db = IConsoleDatabase.getDatabase(application);
        exerciseProfileDao = db.exerciseProfileDao();
        exerciseProfileDataDao = db.exerciseProfileDataDao();
        settingsDao = db.settingsDao();
        userDao = db.userDao();
        exerciseDAO = db.exerciseDAO();
        exerciseDataDao = db.exerciseDataDao();
        exerciseSamplesDao = db.exerciseSamplesDao();

        mAllExerciseProfiles = exerciseProfileDao.getExerciseProfile();
        mAllExerciseProfileData = exerciseProfileDataDao.getExerciseProfileData();
    }

    /**
     * Exercise Profile items
     */
    public LiveData<List<ExerciseProfile>> getAllExerciseProfiles() {
        return mAllExerciseProfiles;
    }

    public Future<Long> insertExerciseProfile(ExerciseProfile exerciseProfile) {
        Future<Long> future = IConsoleDatabase.databaseWriteExecutor.submit(() -> {
            return exerciseProfileDao.insert(exerciseProfile);
        });
        return future;
    }

    public void updateExerciseProfile(ExerciseProfile exerciseProfile) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            exerciseProfileDao.update(exerciseProfile);
        });

    }

    public void deleteExerciseProfile(ExerciseProfile exerciseProfile) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            exerciseProfileDao.delete(exerciseProfile);

        });
    }

    /**
     * Exercise Profile Data items
     */

    public Future<List<ExerciseProfileData>> getExerciseProfileData(Long exerciseProfileID) {
        Future<List<ExerciseProfileData>> profileDataListFuture = IConsoleDatabase.databaseWriteExecutor.submit(() -> {
            return exerciseProfileDataDao.getExerciseProfileDataForID(exerciseProfileID);
        });
        return profileDataListFuture;
    }

    public LiveData<List<ExerciseProfileData>> getExerciseProfileDataLive(Long exerciseProfileID) {
        return exerciseProfileDataDao.getExerciseProfileDataForIDLive(exerciseProfileID);
    }

    LiveData<List<ExerciseProfileData>> getAllExerciseProfileData() {
        return mAllExerciseProfileData;
    }

    public void insertExerciseProfileDataList(List<ExerciseProfileData> exerciseProfileDataList) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            exerciseProfileDataDao.insertList(exerciseProfileDataList);
        });
    }

    public void updateExerciseProfileDataList(List<ExerciseProfileData> exerciseProfileDataList) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            exerciseProfileDataDao.updateList(exerciseProfileDataList);
        });
    }

    public void deleteExerciseProfileDataList(List<ExerciseProfileData> exerciseProfileDataList) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            exerciseProfileDataDao.deleteList(exerciseProfileDataList);
        });
    }

    /**
     * Settings
     **/
    public LiveData<List<Settings>> getSettings(long userID) {
        return settingsDao.getSettings(userID);
    }

    public void insertSettings(Settings settings) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            settingsDao.insert(settings);
        });
    }

    public void updateSettings(Settings settings) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            settingsDao.update(settings);
        });
    }

    public void deleteSettings(Settings settings) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            settingsDao.delete(settings);
        });
    }

    /**
     * Exercise
     **/
    public LiveData<List<Exercise>> getExercises(long userID) {
        return exerciseDAO.getExercise(userID);
    }

    public Future<List<Exercise>> getExercisesNotLive(long userID) {
        Future<List<Exercise>> future = IConsoleDatabase.databaseWriteExecutor.submit(new Callable<List<Exercise>>() {
            @Override
            public List<Exercise> call() throws Exception {
                return exerciseDAO.getExerciseNotLive(userID);
            }
        });
        return future;
    }

    public static void setRecordingExerciseID(long exerciseID) {
        sRecordingExerciseID = exerciseID;
    }

    public Future<Long> insertExercise(Exercise exercise) {
        Future<Long> future = IConsoleDatabase.databaseWriteExecutor.submit(() -> {
            return exerciseDAO.insert(exercise);
        });
        return future;
    }

    public void updateExercise(Exercise exercise) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            exerciseDAO.update(exercise);
        });
    }

    public void deleteExercise(Exercise exercise) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> db.runInTransaction(() -> {
            exerciseDataDao.deleteDataForExercise(exercise.getExerciseID());
            exerciseSamplesDao.deleteForExercise(exercise.getExerciseID());
            exerciseDAO.delete(exercise);
        }));
    }

    /**
     * ExerciseData
     **/
    public LiveData<List<ExerciseData>> getExerciseData(long exerciseID) {
        return exerciseDataDao.getExerciseData(exerciseID);
    }

    /** All samples of a workout, compacted ones first, then rows not compacted yet. */
    public Future<List<ExerciseData>> getExerciseDataNotLive(long exerciseID) {
        return IConsoleDatabase.databaseWriteExecutor.submit(() -> loadAllSamples(exerciseID));
    }

    public void insertExerciseData(ExerciseData exerciseData) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            exerciseDataDao.insert(exerciseData);
        });
    }

    public void updateExerciseData(ExerciseData exerciseData) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            exerciseDataDao.update(exerciseData);
        });
    }

    public void deleteExerciseData(ExerciseData exerciseData) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            exerciseDataDao.delete(exerciseData);
        });
    }

    /**
     * Users
     **/
    public LiveData<List<User>> getUsers() {
        return userDao.getUsers();
    }

    public void insertUser(User user) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            userDao.insert(user);
        });
    }

    public void updateUser(User user) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            userDao.update(user);
        });
    }

    public void deleteUser(User user) {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            userDao.delete(user);
        });
    }

    /**
     * Database statistics
     **/
    public LiveData<Integer> getExerciseDataCount() {
        return exerciseDataDao.getExerciseDataCount();
    }

    public LiveData<Integer> getExerciseCount() {
        return exerciseDAO.getExerciseCount();
    }

    public LiveData<Integer> getSummarizedExerciseCount() {
        return exerciseDAO.getSummarizedExerciseCount();
    }

    public LiveData<Integer> getUnsummarizedExerciseCount() {
        return exerciseDAO.getUnsummarizedExerciseCount();
    }

    public LiveData<List<Exercise>> getAllExercisesLive() {
        return exerciseDAO.getAllExercisesLive();
    }

    public LiveData<List<Exercise>> getAllExercisesChronologicalLive() {
        return exerciseDAO.getAllExercisesChronologicalLive();
    }

    public long getDatabaseSizeInBytes(Context context) {
        long totalSize = 0;
        File dbFile = context.getDatabasePath("IConsoleDB");
        if (dbFile.exists()) {
            totalSize += dbFile.length();
        }
        File walFile = new File(dbFile.getPath() + "-wal");
        if (walFile.exists()) {
            totalSize += walFile.length();
        }
        File shmFile = new File(dbFile.getPath() + "-shm");
        if (shmFile.exists()) {
            totalSize += shmFile.length();
        }
        return totalSize;
    }

    /**
     * empty WALL file, so the DB can be backed up
     **/
    public void emptyWall() {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            settingsDao.checkpoint(new SimpleSQLiteQuery("pragma wal_checkpoint(full)"));
        });
    }

    public LiveData<Long> getCompactedSampleCount() {
        return exerciseSamplesDao.getCompactedSampleCount();
    }

    /** Must run on a DB thread. */
    private List<ExerciseData> loadAllSamples(long exerciseID) {
        List<ExerciseData> all = new ArrayList<>();
        ExerciseSamples packed = exerciseSamplesDao.get(exerciseID);
        if (packed != null)
            all.addAll(SampleCodec.decode(packed.getData(), exerciseID));
        all.addAll(exerciseDataDao.getExerciseDataNotLive(exerciseID));
        return all;
    }

    /**
     * Moves a workout's sample rows into its compact blob, in one transaction, and summarizes it
     * if it isn't yet. The rows are only deleted after the new blob was decoded again and matched
     * the samples exactly; otherwise the transaction is rolled back and nothing changes.
     *
     * @param stopTime stop time to record when summarizing, or null to derive it from the
     *                 start time and duration (for workouts compacted long after they ended)
     * @return the exercise (null if it doesn't exist; orphan rows are still compacted)
     */
    private Exercise compactExercise(long exerciseID, Long stopTime) {
        return db.runInTransaction(() -> {
            List<ExerciseData> rows = exerciseDataDao.getExerciseDataNotLive(exerciseID);
            List<ExerciseData> all = loadAllSamples(exerciseID);

            if (!rows.isEmpty()) {
                byte[] blob = SampleCodec.encode(all);
                if (!SampleCodec.sameSamples(all, SampleCodec.decode(blob, exerciseID)))
                    throw new IllegalStateException("Compacted samples of exercise " + exerciseID + " don't match, keeping rows");
                exerciseSamplesDao.upsert(new ExerciseSamples(exerciseID, all.size(), SampleCodec.VERSION, blob));
                exerciseDataDao.deleteDataForExercise(exerciseID);
            }

            Exercise exercise = exerciseDAO.getExerciseByIDNotLive(exerciseID);
            // Never overwrite an existing summary: its samples may be gone (deleted by older builds).
            if (exercise != null && !exercise.isSummarized()) {
                WorkoutSummary summary = WorkoutSummary.of(all);
                if (summary != null)
                    summary.applyTo(exercise);
                exercise.setExStopDateTime(stopTime != null ? stopTime
                        : exercise.getExStartDateTime() + exercise.getTotalDuration() * 1000);
                exercise.setSummarized(true);
                exerciseDAO.update(exercise);
            }
            return exercise;
        });
    }

    /** Called when a workout is stopped: summarize it and compact its samples. */
    public Future<Exercise> finishExercise(long exerciseID) {
        long stopTime = System.currentTimeMillis();
        if (sRecordingExerciseID == exerciseID)
            sRecordingExerciseID = -1;
        return IConsoleDatabase.databaseWriteExecutor.submit(() -> compactExercise(exerciseID, stopTime));
    }

    /**
     * Compacts every workout that still has sample rows, then VACUUMs so the file actually
     * shrinks (SQLite keeps freed pages otherwise).
     *
     * @return number of workouts compacted
     * @throws IOException (wrapped in the Future) if there's not enough free space to VACUUM;
     *                     the compaction itself has been done and is kept in that case
     */
    public Future<Integer> compactAllExercises(Context context) {
        return IConsoleDatabase.databaseWriteExecutor.submit(() -> {
            int count = 0;
            for (long id : exerciseDataDao.getExerciseIDsWithRows()) {
                if (id == sRecordingExerciseID)
                    continue; // still recording: Stop will summarize and compact it
                compactExercise(id, null);
                count++;
            }
            vacuum(context);
            return count;
        });
    }

    /** Must run on a DB thread, outside a transaction. */
    private void vacuum(Context context) throws IOException {
        SupportSQLiteDatabase sqlite = db.getOpenHelper().getWritableDatabase();
        long pageSize = pragmaLong(sqlite, "PRAGMA page_size");
        long usedBytes = (pragmaLong(sqlite, "PRAGMA page_count") - pragmaLong(sqlite, "PRAGMA freelist_count")) * pageSize;
        // VACUUM writes a full copy of the live data (temp file + WAL), keep a margin on top.
        long needed = 2 * usedBytes + 20L * 1024 * 1024;
        long free = context.getDatabasePath("IConsoleDB").getParentFile().getUsableSpace();
        if (free < needed)
            throw new IOException("Not enough free space to shrink the database: need "
                    + needed / (1024 * 1024) + " MB, have " + free / (1024 * 1024) + " MB");
        Log.i(TAG, "VACUUM, live data " + usedBytes / 1024 + " KB");
        sqlite.execSQL("VACUUM");
        pragmaLong(sqlite, "PRAGMA wal_checkpoint(TRUNCATE)");
    }

    private static long pragmaLong(SupportSQLiteDatabase sqlite, String pragma) {
        try (Cursor c = sqlite.query(pragma)) {
            return c.moveToFirst() ? c.getLong(0) : 0;
        }
    }

}
