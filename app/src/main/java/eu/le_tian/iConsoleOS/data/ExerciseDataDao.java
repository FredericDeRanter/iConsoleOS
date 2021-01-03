package eu.le_tian.iConsoleOS.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
import java.util.concurrent.Future;

@Dao
public interface ExerciseDataDao {
    @Insert()
    long insert(ExerciseData exerciseData);

    @Update()
    void update(ExerciseData exerciseData);

    @Delete()
    void delete(ExerciseData exerciseData);

    @Query("DELETE FROM ExerciseData")
    void deleteAll();

    @Query("SELECT * FROM ExerciseData WHERE exerciseParentID = :exerciseParentID")
    LiveData<List<ExerciseData>> getExerciseData(long exerciseParentID);

    @Query("SELECT * FROM ExerciseData WHERE exerciseParentID = :exerciseParentID")
    List<ExerciseData> getExerciseDataNotLive(long exerciseParentID);

}
