package eu.le_tian.iConsoleOS.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExerciseProfileDao {
    @Insert()
    long insert(ExerciseProfile exerciseProfile);

    @Update()
    void update(ExerciseProfile exerciseProfile);

    @Delete()
    void delete(ExerciseProfile exerciseProfile);

    @Query("DELETE FROM ExerciseProfile")
    void deleteAll();

    @Query("SELECT * FROM ExerciseProfile ORDER BY sortOrder asc, exerciseProfileID desc")
    LiveData<List<ExerciseProfile>> getExerciseProfile();

    @Query("SELECT * FROM ExerciseProfile ORDER BY sortOrder asc, exerciseProfileID desc")
    List<ExerciseProfile> getExerciseProfileNoLive();
}
