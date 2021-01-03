package eu.le_tian.iConsoleOS.data;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;

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
     * empty WALL file, so the DB can be backed up
     **/
    public void emptyWall() {
        IConsoleDatabase.databaseWriteExecutor.execute(() -> {
            settingsDao.checkpoint(new SimpleSQLiteQuery("pragma wal_checkpoint(full)"));
        });
    }

}
