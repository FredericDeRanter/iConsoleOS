package eu.le_tian.iConsoleOS.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExerciseProfileDataDao {
    @Insert()
    long insert(ExerciseProfileData exerciseProfileData);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertList(List<ExerciseProfileData> exerciseProfileDataList);

    @Update()
    void update(ExerciseProfileData exerciseProfileData);

    @Update()
    void updateList(List<ExerciseProfileData> exerciseProfileDataList);

    @Delete()
    void delete(ExerciseProfileData exerciseProfileData);

    @Delete()
    void deleteList(List<ExerciseProfileData> exerciseProfileDataList);

    @Query("DELETE FROM ExerciseProfileData")
    void deleteAll();

    @Query("SELECT * FROM ExerciseProfileData")
    public LiveData<List<ExerciseProfileData>> getExerciseProfileData();

    @Query("SELECT * FROM ExerciseProfileData WHERE parentExerciseProfileID =  :exerciseProfileID ORDER BY dataType asc, sortOrder asc, exerciseProfileDataID asc")
    public List<ExerciseProfileData> getExerciseProfileDataForID(Long exerciseProfileID);

    @Query("SELECT * FROM ExerciseProfileData WHERE parentExerciseProfileID =  :exerciseProfileID ORDER BY dataType asc, sortOrder asc, exerciseProfileDataID asc")
    public LiveData<List<ExerciseProfileData>> getExerciseProfileDataForIDLive(Long exerciseProfileID);

    @Transaction
    @Query("SELECT * FROM ExerciseProfile WHERE exerciseProfileID IN (SELECT DISTINCT (parentExerciseProfileID) FROM ExerciseProfileData)")
    public List<ExerciseProfileWithExerciseProfileData> getExerciseProfileWithExerciseProfileData();
}
