package eu.le_tian.iConsoleOS.data;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class ExerciseProfileWithExerciseProfileData {
    @Embedded
    public ExerciseProfile exerciseProfile;
    @Relation(
            parentColumn = "exerciseProfileID",
            entityColumn = "parentExerciseProfileID"
    )
    public List<ExerciseProfileData> exerciseProfileDataList;
}
