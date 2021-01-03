package eu.le_tian.iConsoleOS.data;

import androidx.room.Embedded;
import androidx.room.Relation;

public class UserWithSettings {
    @Embedded
    public User user;
    @Relation(
            parentColumn = "userID",
            entityColumn = "userParentID"
    )
    public Settings userSettings;
}
