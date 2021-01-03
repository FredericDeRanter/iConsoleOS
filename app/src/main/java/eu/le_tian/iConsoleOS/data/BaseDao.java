package eu.le_tian.iConsoleOS.data;


import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Update;

public interface BaseDao<T> {
    @Insert
    void insert(T Object);

    @Update
    void update(T object);

    @Delete
    void delete(T object);
}
