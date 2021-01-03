package eu.le_tian.iConsoleOS.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Settings")
public class Settings {
    @PrimaryKey(autoGenerate = true)
    private long settingsID;
    private long userParentID;
    private long defaultProfileID;
    private int quickIncrease;

    public Settings(long settingsID, long userParentID, long defaultProfileID, int quickIncrease) {
        this.settingsID = settingsID;
        this.userParentID = userParentID;
        this.defaultProfileID = defaultProfileID;
        this.quickIncrease = quickIncrease;
    }

    public long getSettingsID() {
        return settingsID;
    }

    public void setSettingsID(long settingsID) {
        this.settingsID = settingsID;
    }

    public long getUserParentID() {
        return userParentID;
    }

    public void setUserParentID(long userParentID) {
        this.userParentID = userParentID;
    }

    public long getDefaultProfileID() {
        return defaultProfileID;
    }

    public void setDefaultProfileID(long defaultProfileID) {
        this.defaultProfileID = defaultProfileID;
    }

    public int getQuickIncrease() {
        return quickIncrease;
    }

    public void setQuickIncrease(int quickIncrease) {
        this.quickIncrease = quickIncrease;
    }
}
