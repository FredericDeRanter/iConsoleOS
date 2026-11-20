package eu.le_tian.iConsoleOS.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExerciseDAO {
    @Insert()
    long insert(Exercise exercise);

    @Update()
    void update(Exercise exercise);

    @Delete()
    void delete(Exercise exercise);

    @Query("DELETE FROM Exercise")
    void deleteAll();

    @Query("SELECT * FROM Exercise WHERE userParentID = :userParentID ORDER BY exerciseID asc")
    LiveData<List<Exercise>> getExercise(long userParentID);

    @Query("SELECT * FROM Exercise WHERE userParentID = :userParentID ORDER BY exerciseID asc")
    List<Exercise> getExerciseNotLive(long userParentID);

    @Query("SELECT COUNT(*) FROM Exercise")
    LiveData<Integer> getExerciseCount();

    @Query("SELECT COUNT(*) FROM Exercise WHERE summarized = 1")
    LiveData<Integer> getSummarizedExerciseCount();

    @Query("SELECT COUNT(*) FROM Exercise WHERE summarized = 0")
    LiveData<Integer> getUnsummarizedExerciseCount();

    @Query("SELECT * FROM Exercise ORDER BY exerciseID DESC LIMIT 50")
    LiveData<List<Exercise>> getAllExercisesLive();

    @Query("SELECT * FROM Exercise ORDER BY exStartDateTime ASC")
    LiveData<List<Exercise>> getAllExercisesChronologicalLive();

    @Query("SELECT * FROM Exercise WHERE exerciseID = :exerciseID")
    Exercise getExerciseByIDNotLive(long exerciseID);

    @Query("SELECT * FROM Exercise WHERE summarized = 0")
    List<Exercise> getUnsummarizedExercisesNotLive();

}
