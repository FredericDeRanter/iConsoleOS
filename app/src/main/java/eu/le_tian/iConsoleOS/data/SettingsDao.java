package eu.le_tian.iConsoleOS.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

@Dao
public interface SettingsDao {
    @Insert()
    long insert(Settings settings);

    @Update()
    void update(Settings settings);

    @Delete()
    void delete(Settings settings);

    @Query("DELETE FROM Settings")
    void deleteAll();

    @Query("SELECT * FROM Settings WHERE userParentID = :userParentID")
    LiveData<List<Settings>> getSettings(long userParentID);

    @RawQuery
    int checkpoint(SupportSQLiteQuery supportSQLiteQuery);
}
