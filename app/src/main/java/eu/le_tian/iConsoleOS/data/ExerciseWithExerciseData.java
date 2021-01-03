package eu.le_tian.iConsoleOS.data;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class ExerciseWithExerciseData {
    @Embedded
    public Exercise exercise;
    @Relation(
            parentColumn = "exerciseID",
            entityColumn = "exerciseParentID"
    )
    public List<ExerciseData> exerciseDataList;
}
