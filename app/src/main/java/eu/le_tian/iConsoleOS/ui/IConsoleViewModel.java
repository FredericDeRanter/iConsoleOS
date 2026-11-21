package eu.le_tian.iConsoleOS.ui;

import android.app.Application;
import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.text.format.Formatter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import eu.le_tian.iConsoleOS.data.Exercise;
import eu.le_tian.iConsoleOS.data.ExerciseData;
import eu.le_tian.iConsoleOS.data.ExerciseProfile;
import eu.le_tian.iConsoleOS.data.ExerciseProfileData;
import eu.le_tian.iConsoleOS.data.IConsoleRepository;
import eu.le_tian.iConsoleOS.data.WorkoutCsv;
import eu.le_tian.iConsoleOS.workout.ProfileTimeline;

public class IConsoleViewModel extends AndroidViewModel {

    private String TAG = IConsoleViewModel.class.getSimpleName();
    private IConsoleRepository mRepository;
    private LiveData<List<ExerciseProfile>> mAllExerciseProfiles;
    public ExerciseProfile selectedExerciseProfile; //does not need to be live, since it's the fragment that sets it.
    private LiveData<List<ExerciseProfileData>> selectedExerciseProfileData;
    private MutableLiveData<Long> selectedExerciseProfileID = new MutableLiveData<Long>();

    private MutableLiveData<List<Point>> timeStampsAndLevelsProfile = new MutableLiveData<List<Point>>();
    public Exercise runningExercise;

    public IConsoleViewModel(Application application) {
        super(application);
        mRepository = new IConsoleRepository(application);
        mAllExerciseProfiles = mRepository.getAllExerciseProfiles();
        selectedExerciseProfileData = Transformations.switchMap(selectedExerciseProfileID, v -> mRepository.getExerciseProfileDataLive(v));
    }

    void setSelectedExerciseProfile(ExerciseProfile exerciseProfile) {
        selectedExerciseProfile = exerciseProfile; //the switchmap will get an updated profiledata list
    }

    public int getMinDuration(List<ExerciseProfileData> exerciseProfileDataList) {
        return ProfileTimeline.minDuration(exerciseProfileDataList);
    }

    /** Publishes the selected profile's level timeline (see {@link ProfileTimeline}) for Home and its graph. */
    public void CreateTimeStampsAndLevels(List<ExerciseProfileData> exerciseProfileDataList, int duration, boolean repeat, int profileDefaultLevel) {
        List<Point> points = new ArrayList<>();
        for (ProfileTimeline.Step step : ProfileTimeline.build(exerciseProfileDataList, duration, repeat, profileDefaultLevel))
            points.add(new Point(step.second, step.level));
        Log.d(TAG, "timestamps and Levels: " + points);
        timeStampsAndLevelsProfile.setValue(points);
    }

    ExerciseProfile getSelectedExerciseProfile() {
        return selectedExerciseProfile;
    }

    LiveData<List<ExerciseProfile>> getAllExerciseProfiles() {
        return mAllExerciseProfiles;
    }

    LiveData<List<ExerciseProfileData>> getAllExerciseProfileDataSelected() {
        return selectedExerciseProfileData;
    }

    public MutableLiveData<List<Point>> getTimeStampsAndLevelsProfile() {
        return timeStampsAndLevelsProfile;
    }

    void setSelectedExerciseProfileID(Long ID) {
        selectedExerciseProfileID.setValue(ID);
    }

    Future<List<ExerciseProfileData>> getExerciseProfileDataList(Long exerciseProfileID) {
        return mRepository.getExerciseProfileData(exerciseProfileID);
    }

    Future<Long> insertExerciseProfile(ExerciseProfile exerciseProfile) {
        return mRepository.insertExerciseProfile(exerciseProfile);
    }

    void updateExerciseProfile(ExerciseProfile exerciseProfile) {
        mRepository.updateExerciseProfile(exerciseProfile);
    }

    void deleteExerciseProfile(ExerciseProfile exerciseProfile) {
        mRepository.deleteExerciseProfile(exerciseProfile);
    }

    void insertExerciseProfileDataList(List<ExerciseProfileData> exerciseProfileDataList) {
        mRepository.insertExerciseProfileDataList(exerciseProfileDataList);
    }

    void deleteExerciseProfileDataList(List<ExerciseProfileData> exerciseProfileDataList) {
        mRepository.deleteExerciseProfileDataList(exerciseProfileDataList);
    }

    void updateExerciseProfileDataList(List<ExerciseProfileData> exerciseProfileDataList) {
        mRepository.updateExerciseProfileDataList(exerciseProfileDataList);
    }

    void emptyWall() {
        mRepository.emptyWall();
    }

    /** Ride being recorded, or null. */
    private IConsoleRepository.Recording mRecording;

    /** Start recording a ride. Doesn't block: the row is inserted on the recording thread. */
    void startExercise(Exercise exercise) {
        runningExercise = exercise;
        mRecording = mRepository.startRecording(exercise);
    }

    /** Store one sample of the running ride (its exercise ID is filled in when stored). */
    void insertExerciseData(ExerciseData exerciseData) {
        IConsoleRepository.Recording r = mRecording;
        if (r != null)
            mRepository.recordSample(r, exerciseData);
    }

    Future<List<Exercise>> getExercise(long userID) {
        return mRepository.getExercisesNotLive(userID);
    }

    Future<List<ExerciseData>> getExerciseData(long exerciseID) {
        return mRepository.getExerciseDataNotLive(exerciseID);
    }

    /** Stop: summarize and compact the ride once all its samples are stored. Null if none. */
    Future<Exercise> finishExercise() {
        IConsoleRepository.Recording r = mRecording;
        mRecording = null;
        return r != null ? mRepository.finishRecording(r) : null;
    }

    Future<Integer> compactAllExercises() {
        return mRepository.compactAllExercises(getApplication());
    }

    LiveData<Long> getCompactedSampleCount() {
        return mRepository.getCompactedSampleCount();
    }

    /** CSV of all workouts of user 1 to a file chosen with the file picker; in the background. */
    Future<WorkoutCsv.Result> exportCsv(Uri target) {
        return mRepository.exportCsv(getApplication(), target, 1);
    }

    public LiveData<Integer> getExerciseDataCount() {
        return mRepository.getExerciseDataCount();
    }

    public LiveData<Integer> getExerciseCount() {
        return mRepository.getExerciseCount();
    }

    public LiveData<Integer> getSummarizedExerciseCount() {
        return mRepository.getSummarizedExerciseCount();
    }

    public LiveData<Integer> getUnsummarizedExerciseCount() {
        return mRepository.getUnsummarizedExerciseCount();
    }

    public LiveData<List<Exercise>> getAllExercisesLive() {
        return mRepository.getAllExercisesLive();
    }

    public LiveData<List<Exercise>> getAllExercisesChronologicalLive() {
        return mRepository.getAllExercisesChronologicalLive();
    }

    public String getFormattedDatabaseSize() {
        long bytes = mRepository.getDatabaseSizeInBytes(getApplication());
        return Formatter.formatShortFileSize(getApplication(), bytes);
    }

}