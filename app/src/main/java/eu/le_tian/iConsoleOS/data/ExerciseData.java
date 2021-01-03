package eu.le_tian.iConsoleOS.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ExerciseData")
public class ExerciseData {
    @PrimaryKey(autoGenerate = true)
    private long exerciseDataID;
    private int exPower; //power * 10
    private int exSpeed; //speed * 10
    private int exDistance; //distance * 10
    private int exCalories;
    private int exHR;
    private int exRPM;
    private long exTimestamp; //in seconds
    private long exerciseParentID;

    public ExerciseData(long exerciseDataID, int exPower, int exSpeed, int exDistance, int exCalories, int exHR, int exRPM, long exTimestamp, long exerciseParentID) {
        this.exerciseDataID = exerciseDataID;
        this.exPower = exPower;
        this.exSpeed = exSpeed;
        this.exDistance = exDistance;
        this.exCalories = exCalories;
        this.exHR = exHR;
        this.exRPM = exRPM;
        this.exTimestamp = exTimestamp;
        this.exerciseParentID = exerciseParentID;
    }

    public long getExerciseDataID() {
        return exerciseDataID;
    }

    public void setExerciseDataID(long exerciseDataID) {
        this.exerciseDataID = exerciseDataID;
    }

    public int getExPower() {
        return exPower;
    }

    public void setExPower(int exPower) {
        this.exPower = exPower;
    }

    public int getExSpeed() {
        return exSpeed;
    }

    public void setExSpeed(int exSpeed) {
        this.exSpeed = exSpeed;
    }

    public int getExDistance() {
        return exDistance;
    }

    public void setExDistance(int exDistance) {
        this.exDistance = exDistance;
    }

    public int getExCalories() {
        return exCalories;
    }

    public void setExCalories(int exCalories) {
        this.exCalories = exCalories;
    }

    public int getExHR() {
        return exHR;
    }

    public void setExHR(int exHR) {
        this.exHR = exHR;
    }

    public int getExRPM() {
        return exRPM;
    }

    public void setExRPM(int exRPM) {
        this.exRPM = exRPM;
    }

    public long getExTimestamp() {
        return exTimestamp;
    }

    public void setExTimestamp(long exTimestamp) {
        this.exTimestamp = exTimestamp;
    }

    public long getExerciseParentID() {
        return exerciseParentID;
    }

    public void setExerciseParentID(long exerciseParentID) {
        this.exerciseParentID = exerciseParentID;
    }
}

