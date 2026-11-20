package eu.le_tian.iConsoleOS.data;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class IConsoleRepository {
    private static String TAG = "Repository";
    private ExerciseProfileDao exerciseProfileDao;
    private ExerciseProfileDataDao exerciseProfileDataDao;
    private SettingsDao settingsDao;
    private UserDao userDao;
    private ExerciseDAO exerciseDAO;
    private ExerciseDataDao exerciseDataDao;
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
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            exerciseDAO.delete(exercise);
        });
    }

    /**
     * ExerciseData
     **/
    public LiveData<List<ExerciseData>> getExerciseData(long exerciseID) {
        return exerciseDataDao.getExerciseData(exerciseID);
    }

    public Future<List<ExerciseData>> getExerciseDataNotLive(long exerciseID) {
        Future<List<ExerciseData>> future = IConsoleDatabase.databaseWriteExecutor.submit(new Callable<List<ExerciseData>>() {
            @Override
            public List<ExerciseData> call() throws Exception {
                return exerciseDataDao.getExerciseDataNotLive(exerciseID);
            }

        });
        return future;
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

    private Exercise summarizeAndCleanExerciseInternal(Exercise exercise) {
        if (exercise == null) {
            return null;
        }

        List<ExerciseData> dataList = exerciseDataDao.getExerciseDataNotLive(exercise.getExerciseID());
        if (dataList != null && !dataList.isEmpty()) {
            int maxPower = 0, sumPower = 0;
            int maxSpeed = 0, sumSpeed = 0;
            int maxHR = 0, sumHR = 0;
            int maxRPM = 0, sumRPM = 0;
            int maxDistance = 0, maxCalories = 0;

            for (ExerciseData d : dataList) {
                if (d.getExPower() > maxPower) maxPower = d.getExPower();
                sumPower += d.getExPower();

                if (d.getExSpeed() > maxSpeed) maxSpeed = d.getExSpeed();
                sumSpeed += d.getExSpeed();

                if (d.getExHR() > maxHR) maxHR = d.getExHR();
                sumHR += d.getExHR();

                if (d.getExRPM() > maxRPM) maxRPM = d.getExRPM();
                sumRPM += d.getExRPM();

                if (d.getExDistance() > maxDistance) maxDistance = d.getExDistance();
                if (d.getExCalories() > maxCalories) maxCalories = d.getExCalories();
            }

            int count = dataList.size();
            long duration = dataList.get(count - 1).getExTimestamp();

            exercise.setTotalDuration(duration);
            exercise.setTotalDistance(maxDistance);
            exercise.setTotalCalories(maxCalories);
            exercise.setAvgPower(count > 0 ? sumPower / count : 0);
            exercise.setMaxPower(maxPower);
            exercise.setAvgSpeed(count > 0 ? sumSpeed / count : 0);
            exercise.setMaxSpeed(maxSpeed);
            exercise.setAvgHR(count > 0 ? sumHR / count : 0);
            exercise.setMaxHR(maxHR);
            exercise.setAvgRPM(count > 0 ? sumRPM / count : 0);
            exercise.setMaxRPM(maxRPM);
        }

        exercise.setExStopDateTime(System.currentTimeMillis());
        exercise.setSummarized(true);

        exerciseDAO.update(exercise);
        exerciseDataDao.deleteDataForExercise(exercise.getExerciseID());

        return exercise;
    }

    public Future<Exercise> summarizeAndCleanExercise(long exerciseID) {
        return IConsoleDatabase.databaseWriteExecutor.submit(() -> {
            Exercise exercise = exerciseDAO.getExerciseByIDNotLive(exerciseID);
            return summarizeAndCleanExerciseInternal(exercise);
        });
    }

    public Future<Integer> summarizeAllUnsummarizedExercises() {
        return IConsoleDatabase.databaseWriteExecutor.submit(() -> {
            List<Exercise> unsummarizedList = exerciseDAO.getUnsummarizedExercisesNotLive();
            if (unsummarizedList == null || unsummarizedList.isEmpty()) {
                return 0;
            }
            int processedCount = 0;
            for (Exercise ex : unsummarizedList) {
                summarizeAndCleanExerciseInternal(ex);
                processedCount++;
            }
            return processedCount;
        });
    }

}
