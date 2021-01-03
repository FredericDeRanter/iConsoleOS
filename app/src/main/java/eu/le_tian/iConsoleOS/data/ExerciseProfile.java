package eu.le_tian.iConsoleOS.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ExerciseProfile")
public class ExerciseProfile {
    @PrimaryKey(autoGenerate = true)
    private long exerciseProfileID;
    private long sortOrder;
    private String exerciseProfileName;
    private int startLevel;
    private int defaultDuration; //in seconds
    private boolean repeatToFill; //decides if relative or absolute time for markers (if relative, all steps will be extended to fill the total duration else the exercise steps will just keep repeating to fill up duration

    public ExerciseProfile(long exerciseProfileID, long sortOrder, String exerciseProfileName, int startLevel, int defaultDuration, boolean repeatToFill) {
        this.exerciseProfileID = exerciseProfileID;
        this.sortOrder = sortOrder;
        this.exerciseProfileName = exerciseProfileName;
        this.startLevel = startLevel;
        this.defaultDuration = defaultDuration;
        this.repeatToFill = repeatToFill;
    }

    public long getExerciseProfileID() {
        return exerciseProfileID;
    }

    public void setExerciseProfileID(long exerciseProfileID) {
        this.exerciseProfileID = exerciseProfileID;
    }

    public String getExerciseProfileName() {
        return exerciseProfileName;
    }

    public void setExerciseProfileName(String exerciseProfileName) {
        this.exerciseProfileName = exerciseProfileName;
    }

    public int getStartLevel() {
        return startLevel;
    }

    public void setStartLevel(int startLevel) {
        this.startLevel = startLevel;
    }

    public int getDefaultDuration() {
        return defaultDuration;
    }

    public void setDefaultDuration(int defaultDuration) {
        this.defaultDuration = defaultDuration;
    }

    public boolean isRepeatToFill() {
        return repeatToFill;
    }

    public void setRepeatToFill(boolean repeatToFill) {
        this.repeatToFill = repeatToFill;
    }

    public long getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(long sortOrder) {
        this.sortOrder = sortOrder;
    }

    @NonNull
    @Override
    public String toString() {
        return exerciseProfileName;
    }
}
