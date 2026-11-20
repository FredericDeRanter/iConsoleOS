package eu.le_tian.iConsoleOS.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ExerciseSamplesDao {
    /** Replaces an existing blob: compaction re-packs old blob + new rows into one. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ExerciseSamples samples);

    @Query("SELECT * FROM ExerciseSamples WHERE exerciseID = :exerciseID")
    ExerciseSamples get(long exerciseID);

    @Query("DELETE FROM ExerciseSamples WHERE exerciseID = :exerciseID")
    void deleteForExercise(long exerciseID);

    @Query("SELECT COALESCE(SUM(sampleCount), 0) FROM ExerciseSamples")
    LiveData<Long> getCompactedSampleCount();
}
