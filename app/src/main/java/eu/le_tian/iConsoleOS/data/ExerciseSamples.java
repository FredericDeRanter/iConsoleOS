package eu.le_tian.iConsoleOS.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * All samples of one finished workout, packed by {@link SampleCodec} (added in DB version 8).
 * While a workout is recording its samples live as rows in {@link ExerciseData}; compaction moves
 * them here, so a workout's full sample list is this blob followed by any remaining rows.
 */
@Entity(tableName = "ExerciseSamples")
public class ExerciseSamples {
    @PrimaryKey
    private long exerciseID;
    private int sampleCount;
    private int codecVersion;
    @NonNull
    private byte[] data;

    public ExerciseSamples(long exerciseID, int sampleCount, int codecVersion, @NonNull byte[] data) {
        this.exerciseID = exerciseID;
        this.sampleCount = sampleCount;
        this.codecVersion = codecVersion;
        this.data = data;
    }

    public long getExerciseID() {
        return exerciseID;
    }

    public void setExerciseID(long exerciseID) {
        this.exerciseID = exerciseID;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public int getCodecVersion() {
        return codecVersion;
    }

    public void setCodecVersion(int codecVersion) {
        this.codecVersion = codecVersion;
    }

    @NonNull
    public byte[] getData() {
        return data;
    }

    public void setData(@NonNull byte[] data) {
        this.data = data;
    }
}
