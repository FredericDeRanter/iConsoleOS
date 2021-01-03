package eu.le_tian.iConsoleOS.data;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ExerciseProfileData")
public class ExerciseProfileData {
    @PrimaryKey(autoGenerate = true)
    private long exerciseProfileDataID;
    private long sortOrder;
    private int dataType; //1 = warm-up, 2 = exercise; 3 = cool-down
    private int relativeLevel;
    private int duration;
    private long parentExerciseProfileID;

    public ExerciseProfileData(long exerciseProfileDataID, long sortOrder, int dataType, int relativeLevel, int duration, long parentExerciseProfileID) {
        this.exerciseProfileDataID = exerciseProfileDataID;
        this.sortOrder = sortOrder;
        this.relativeLevel = relativeLevel;
        this.dataType = dataType;
        this.duration = duration;
        this.parentExerciseProfileID = parentExerciseProfileID;
    }

    public long getExerciseProfileDataID() {
        return exerciseProfileDataID;
    }

    public void setExerciseProfileDataID(long exerciseProfileDataID) {
        this.exerciseProfileDataID = exerciseProfileDataID;
    }

    public int getRelativeLevel() {
        return relativeLevel;
    }

    public void setRelativeLevel(int relativeLevel) {
        this.relativeLevel = relativeLevel;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getParentExerciseProfileID() {
        return parentExerciseProfileID;
    }

    public void setParentExerciseProfileID(long parentExerciseProfileID) {
        this.parentExerciseProfileID = parentExerciseProfileID;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public long getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(long sortOrder) {
        this.sortOrder = sortOrder;
    }
}
