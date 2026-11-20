package eu.le_tian.iConsoleOS.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "Exercise")
public class Exercise {
    @PrimaryKey(autoGenerate = true)
    private long exerciseID;
    private long exStartDateTime;
    private long exStopDateTime;
    private String exProfileName;
    private long userParentID;

    private boolean summarized;
    private long totalDuration; // in seconds
    private int totalDistance;  // distance * 10
    private int totalCalories;
    private int avgPower;
    private int maxPower;
    private int avgSpeed;
    private int maxSpeed;
    private int avgHR;
    private int maxHR;
    private int avgRPM;
    private int maxRPM;

    public Exercise(long exerciseID, long exStartDateTime, long exStopDateTime, String exProfileName, long userParentID) {
        this.exerciseID = exerciseID;
        this.exStartDateTime = exStartDateTime;
        this.exStopDateTime = exStopDateTime;
        this.exProfileName = exProfileName;
        this.userParentID = userParentID;
        this.summarized = false;
    }

    @Ignore
    public Exercise(long exerciseID, long exStartDateTime, long exStopDateTime, String exProfileName, long userParentID,
                    boolean summarized, long totalDuration, int totalDistance, int totalCalories,
                    int avgPower, int maxPower, int avgSpeed, int maxSpeed,
                    int avgHR, int maxHR, int avgRPM, int maxRPM) {
        this.exerciseID = exerciseID;
        this.exStartDateTime = exStartDateTime;
        this.exStopDateTime = exStopDateTime;
        this.exProfileName = exProfileName;
        this.userParentID = userParentID;
        this.summarized = summarized;
        this.totalDuration = totalDuration;
        this.totalDistance = totalDistance;
        this.totalCalories = totalCalories;
        this.avgPower = avgPower;
        this.maxPower = maxPower;
        this.avgSpeed = avgSpeed;
        this.maxSpeed = maxSpeed;
        this.avgHR = avgHR;
        this.maxHR = maxHR;
        this.avgRPM = avgRPM;
        this.maxRPM = maxRPM;
    }

    public long getExerciseID() {
        return exerciseID;
    }

    public void setExerciseID(long exerciseID) {
        this.exerciseID = exerciseID;
    }

    public long getExStartDateTime() {
        return exStartDateTime;
    }

    public void setExStartDateTime(long exStartDateTime) {
        this.exStartDateTime = exStartDateTime;
    }

    public long getExStopDateTime() {
        return exStopDateTime;
    }

    public void setExStopDateTime(long exStopDateTime) {
        this.exStopDateTime = exStopDateTime;
    }

    public String getExProfileName() {
        return exProfileName;
    }

    public void setExProfileName(String exProfileName) {
        this.exProfileName = exProfileName;
    }

    public long getUserParentID() {
        return userParentID;
    }

    public void setUserParentID(long userParentID) {
        this.userParentID = userParentID;
    }

    public boolean isSummarized() {
        return summarized;
    }

    public void setSummarized(boolean summarized) {
        this.summarized = summarized;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public int getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(int totalDistance) {
        this.totalDistance = totalDistance;
    }

    public int getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(int totalCalories) {
        this.totalCalories = totalCalories;
    }

    public int getAvgPower() {
        return avgPower;
    }

    public void setAvgPower(int avgPower) {
        this.avgPower = avgPower;
    }

    public int getMaxPower() {
        return maxPower;
    }

    public void setMaxPower(int maxPower) {
        this.maxPower = maxPower;
    }

    public int getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(int avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(int maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public int getAvgHR() {
        return avgHR;
    }

    public void setAvgHR(int avgHR) {
        this.avgHR = avgHR;
    }

    public int getMaxHR() {
        return maxHR;
    }

    public void setMaxHR(int maxHR) {
        this.maxHR = maxHR;
    }

    public int getAvgRPM() {
        return avgRPM;
    }

    public void setAvgRPM(int avgRPM) {
        this.avgRPM = avgRPM;
    }

    public int getMaxRPM() {
        return maxRPM;
    }

    public void setMaxRPM(int maxRPM) {
        this.maxRPM = maxRPM;
    }
}
