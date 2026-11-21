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

    // DB v9, see RideMetrics. 0 = unknown (rides whose samples are gone, or shorter than the window).
    private int normPower;      // Normalized Power * 10
    private int best5min;       // best 5-minute average power * 10
    private int best20min;
    private int best60min;
    private int metricsVersion; // RideMetrics.VERSION these were computed with, 0 = never

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

    public int getNormPower() {
        return normPower;
    }

    public void setNormPower(int normPower) {
        this.normPower = normPower;
    }

    public int getBest5min() {
        return best5min;
    }

    public void setBest5min(int best5min) {
        this.best5min = best5min;
    }

    public int getBest20min() {
        return best20min;
    }

    public void setBest20min(int best20min) {
        this.best20min = best20min;
    }

    public int getBest60min() {
        return best60min;
    }

    public void setBest60min(int best60min) {
        this.best60min = best60min;
    }

    public int getMetricsVersion() {
        return metricsVersion;
    }

    public void setMetricsVersion(int metricsVersion) {
        this.metricsVersion = metricsVersion;
    }
}
