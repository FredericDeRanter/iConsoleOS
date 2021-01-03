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

//    @Query("SELECT * FROM Exercise WHERE exerciseID = :exerciseID")
//    List<Exercise> getExerciseByID(long exerciseID);

}
