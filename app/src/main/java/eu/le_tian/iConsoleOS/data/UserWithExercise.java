package eu.le_tian.iConsoleOS.data;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class UserWithExercise {
    @Embedded
    public User user;
    @Relation(
            parentColumn = "userID",
            entityColumn = "userParentID"
    )
    public List<Exercise> userExerciseList;
}
