package eu.le_tian.iConsoleOS.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Exercise")
public class Exercise {
    @PrimaryKey(autoGenerate = true)
    private long exerciseID;
    private long exStartDateTime;
    private long exStopDateTime;
    private String exProfileName;
    private long userParentID;

    public Exercise(long exerciseID, long exStartDateTime, long exStopDateTime, String exProfileName, long userParentID) {
        this.exerciseID = exerciseID;
        this.exStartDateTime = exStartDateTime;
        this.exStopDateTime = exStopDateTime;
        this.exProfileName = exProfileName;
        this.userParentID = userParentID;
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
}
