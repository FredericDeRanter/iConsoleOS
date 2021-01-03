package eu.le_tian.iConsoleOS.ui;

import android.app.Application;
import android.content.Context;
import android.graphics.Point;
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

import eu.le_tian.iConsoleOS.CSVclass.OpenCSVWriter;
import eu.le_tian.iConsoleOS.data.Exercise;
import eu.le_tian.iConsoleOS.data.ExerciseData;
import eu.le_tian.iConsoleOS.data.ExerciseProfile;
import eu.le_tian.iConsoleOS.data.ExerciseProfileData;
import eu.le_tian.iConsoleOS.data.IConsoleRepository;

public class IConsoleViewModel extends AndroidViewModel {

    private String TAG = IConsoleViewModel.class.getSimpleName();
    private IConsoleRepository mRepository;
    private LiveData<List<ExerciseProfile>> mAllExerciseProfiles;
    public ExerciseProfile selectedExerciseProfile; //does not need to be live, since it's the fragment that sets it.
    private LiveData<List<ExerciseProfileData>> selectedExerciseProfileData;
    private MutableLiveData<Long> selectedExerciseProfileID = new MutableLiveData<Long>();

    private MutableLiveData<List<Point>> timeStampsAndLevelsProfile = new MutableLiveData<List<Point>>();
    public List<Point> timeStampsAndLevelsDone = new ArrayList<>();
    public int relativeLevel;

    public List<Point> timeStampsAndLevels = new ArrayList<Point>();

    //public List<Integer> timeStamps = new ArrayList<Integer>();
    //public List<Integer> levelForTimeStamps= new ArrayList<Integer>();
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
        int totalDuration = 0;
        for (ExerciseProfileData temp : exerciseProfileDataList) {
            totalDuration += temp.getDuration();
        }
        return totalDuration;
    }


    /**
     * function to create the points of the time and level for a profile
     * check what to do for quick start.
     */
    public void CreateTimeStampsAndLevels(List<ExerciseProfileData> exerciseProfileDataList, int duration, boolean repeat, int profileDefaultLevel) {
        /**
         * if no duration set. It's quick start and we just need the level in the exerciseProfile (profileDefaultLevel)
         * first sum up warmup and cooldown periods. Subtract from total duration; then fill remaining with repeat or extended versions of exercisedata
         *
         * duration can not be shorter than the total of all durations
         * create a temp list and assign it in the end to the MutableLiveData so it can be observed.
         *
         * So this list will always have at least one datapoint.
         *
         */

        List<Point> tempTimeStampsAndLevels = new ArrayList<Point>();

        int totalWarmUp = 0;
        int totalCoolDown = 0;
        int totalExercise = 0;
        int indexOfFirstExercise = 0;
        int indexOfFirstCoolDown = 0;

        //first check if duration = 0
        if (exerciseProfileDataList == null || exerciseProfileDataList.isEmpty()) {
            //set level to profileDefaultLevel
            tempTimeStampsAndLevels.add(new Point(0, profileDefaultLevel));
            //if there is a duration, set a second point to that duration, with the defaultLevel (so that the graph is showing the "whole profile"
            if (duration > 0) {
                tempTimeStampsAndLevels.add(new Point(duration, profileDefaultLevel));
            }

        } else { //the is an exerciseProfile (duration can not be 0)
            for (int i = 0; i < exerciseProfileDataList.size(); i++) {
                int dataType = exerciseProfileDataList.get(i).getDataType();
                if (dataType == 0) {
                    totalWarmUp += exerciseProfileDataList.get(i).getDuration();
                    indexOfFirstExercise = i + 1;
                } else if (dataType == 1) {
                    totalExercise += exerciseProfileDataList.get(i).getDuration();
                    indexOfFirstCoolDown = i + 1;
                } else if (dataType == 2) {
                    totalCoolDown += exerciseProfileDataList.get(i).getDuration();
                }
            }
            int exerciseDuration = duration - totalCoolDown; // - totalWarmUp;
            int movingDuration = 0;
            Log.d(TAG, "totalWarmup: " + totalWarmUp + " | totalExerciseTime: " + totalExercise + " | totalCoolDown:" + totalCoolDown
                    + " | indexOfFirstExercise: " + indexOfFirstExercise + " | indexOfFirstCoolDown: " + indexOfFirstCoolDown);
            if (repeat) {
                for (int i = 0; i < exerciseProfileDataList.size(); i++) {
                    int dataType = exerciseProfileDataList.get(i).getDataType();
                    if (dataType == 0) {
                        //tempDonePoints.add(new Point(0, 10));
                        tempTimeStampsAndLevels.add(new Point(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                        //timeStamps.add(movingDuration);
                        //levelForTimeStamps.add();
                        movingDuration += exerciseProfileDataList.get(i).getDuration(); //120
                    } else if (dataType == 1) {
                        int tempDuration = exerciseProfileDataList.get(i).getDuration(); //300 / 30 / 300 / 30
                        int durationLeftForExercise = exerciseDuration - movingDuration; //2400 - 120 - 300 - 30 - 300 - 30 -300 -30
                        if (durationLeftForExercise > tempDuration) {
                            tempTimeStampsAndLevels.add(new Point(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                            //timeStamps.add(movingDuration);
                            //levelForTimeStamps.add(exerciseProfileDataList.get(i).getRelativeLevel());
                            movingDuration += tempDuration;
                            if (i + 1 == indexOfFirstCoolDown) {
                                //we go back to the first exercise
                                i = indexOfFirstExercise - 1;
                            }
                        } else {
                            tempTimeStampsAndLevels.add(new Point(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                            //timeStamps.add(movingDuration);
                            //levelForTimeStamps.add(exerciseProfileDataList.get(i).getRelativeLevel());
                            movingDuration += durationLeftForExercise;
                            i = indexOfFirstCoolDown - 1;
                        }
                    } else if (dataType == 2) {
                        tempTimeStampsAndLevels.add(new Point(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                        //timeStamps.add(movingDuration);
                        //levelForTimeStamps.add(exerciseProfileDataList.get(i).getRelativeLevel());
                        movingDuration += exerciseProfileDataList.get(i).getDuration();
                    }
                }
            } else {
                for (int i = 0; i < exerciseProfileDataList.size(); i++) {
                    int dataType = exerciseProfileDataList.get(i).getDataType();
                    if (dataType == 0) {
                        tempTimeStampsAndLevels.add(new Point(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                        //timeStamps.add(movingDuration);
                        //levelForTimeStamps.add(exerciseProfileDataList.get(i).getRelativeLevel());
                        movingDuration += exerciseProfileDataList.get(i).getDuration();
                    } else if (dataType == 1) {
                        int tempDuration = exerciseProfileDataList.get(i).getDuration();
                        int durationLeftForExercise = exerciseDuration - movingDuration;
                        int actualDuration = 0;
                        if (i + 1 == indexOfFirstCoolDown) {
                            actualDuration = durationLeftForExercise;
                        } else {
                            int totalDurationForExercise = exerciseDuration - totalWarmUp;
                            double factor = (double) totalDurationForExercise / totalExercise;
                            actualDuration = (int) (tempDuration * factor);
                        }
                        tempTimeStampsAndLevels.add(new Point(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                        //timeStamps.add(movingDuration);
                        //levelForTimeStamps.add(exerciseProfileDataList.get(i).getRelativeLevel());
                        movingDuration += actualDuration;
                    } else if (dataType == 2) {
                        tempTimeStampsAndLevels.add(new Point(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                        //timeStamps.add(movingDuration);
                        //levelForTimeStamps.add(exerciseProfileDataList.get(i).getRelativeLevel());
                        movingDuration += exerciseProfileDataList.get(i).getDuration();
                    }
                }
            }
        }
        Log.d(TAG, "timestamps and Levels: " + tempTimeStampsAndLevels.toString());
        timeStampsAndLevelsProfile.setValue(tempTimeStampsAndLevels);
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

    void insertExercise(Exercise exercise) {
        Future<Long> futureID = mRepository.insertExercise(exercise);
        try {
            exercise.setExerciseID(futureID.get());
        } catch (ExecutionException e) {
            Log.e(TAG, e.toString());
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }
        runningExercise = exercise;
    }

    void insertExerciseData(ExerciseData exerciseData) {
        mRepository.insertExerciseData(exerciseData);
    }

    Future<List<Exercise>> getExercise(long userID) {
        return mRepository.getExercisesNotLive(userID);
    }

    Future<List<ExerciseData>> getExerciseData(long exerciseID) {
        return mRepository.getExerciseDataNotLive(exerciseID);
    }

    void writeExercisesToCSV(Long userID) {

        //TODO add field to only export 1 exercise or only add the ones not exported
        Future<List<Exercise>> exerciseList = getExercise(userID);
        List<List<String>> exercisesToExport = new ArrayList<List<String>>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z");

        try {
            List<Exercise> exercises = exerciseList.get();
            List<String> titleArray = new ArrayList<String>(Arrays.asList("ExerciseID", "ProfileName", "StartDateTime", "UserID", "Calories", "Distance", "HeartRate", "Power", "RPM", "Speed", "TimeStamp"));
            exercisesToExport.add(titleArray);
            boolean appendToFile = false;
            for (int i = 0; i < exercises.size(); i++) {
                Exercise tempEx = exercises.get(i);
                Future<List<ExerciseData>> exerciseDataFuture = getExerciseData(tempEx.getExerciseID());
                //Create a List of all the data
                List<ExerciseData> exerciseDataList = exerciseDataFuture.get();
                for (ExerciseData exerciseData : exerciseDataList) {
                    List<String> tempArray = new ArrayList<String>(Arrays.asList(Long.toString(tempEx.getExerciseID()), tempEx.getExProfileName(), sdf.format(tempEx.getExStartDateTime()), Long.toString(tempEx.getUserParentID()), Integer.toString(exerciseData.getExCalories()),
                            Integer.toString(exerciseData.getExDistance()), Integer.toString(exerciseData.getExHR()), Integer.toString(exerciseData.getExPower()), Integer.toString(exerciseData.getExRPM()), Integer.toString(exerciseData.getExSpeed()), Long.toString(exerciseData.getExTimestamp())));
                    exercisesToExport.add(tempArray);
                }
                OpenCSVWriter.writeLinesToFile("Exercises.csv", exercisesToExport, ',', appendToFile);
                exercisesToExport.clear();
                appendToFile = true;
            }

        } catch (ExecutionException e) {
            Log.e(TAG, e.toString());
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }





    }


}