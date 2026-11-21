package eu.le_tian.iConsoleOS.communication;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Bluetooth / ANT+ settings of this device (SharedPreferences, not the workout database: they
 * are about the tablet, not about the user's data).
 */
public final class BtSettings {
    private static final String PREFS = "bt_health";
    private static final String KEY_ANT = "ant_broadcast";
    private static final String KEY_IDLE = "idle_minutes";
    private static final String KEY_HR = "ant_heart_rate";
    private static final String KEY_HR_DEVICE = "ant_heart_rate_device";
    public static final int DEFAULT_IDLE_MINUTES = 10;
    /** Choices offered in Settings; 0 = never. */
    public static final int[] IDLE_CHOICES = {0, 5, 10, 20, 30};

    private BtSettings() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Broadcast power/speed over ANT+ (for the watch) while the bike is connected. */
    public static boolean antBroadcast(Context c) {
        return prefs(c).getBoolean(KEY_ANT, true);
    }

    public static void setAntBroadcast(Context c, boolean on) {
        prefs(c).edit().putBoolean(KEY_ANT, on).apply();
        BtLog.log("setting: ANT+ broadcast " + (on ? "on" : "off"));
    }

    /** Disconnect the bike after this many minutes without activity; 0 = never. */
    public static int idleMinutes(Context c) {
        return prefs(c).getInt(KEY_IDLE, DEFAULT_IDLE_MINUTES);
    }

    public static void setIdleMinutes(Context c, int minutes) {
        prefs(c).edit().putInt(KEY_IDLE, minutes).apply();
        BtLog.log("setting: idle disconnect " + (minutes == 0 ? "never" : minutes + " min"));
    }

    /** Receive heart rate from an ANT+ monitor (the watch) while the bike is connected. */
    public static boolean heartRateReceive(Context c) {
        return prefs(c).getBoolean(KEY_HR, true);
    }

    public static void setHeartRateReceive(Context c, boolean on) {
        prefs(c).edit().putBoolean(KEY_HR, on).apply();
        BtLog.log("setting: ANT+ heart rate " + (on ? "on" : "off"));
    }

    /** ANT device number of the paired heart-rate monitor; 0 = pair with the first one found. */
    public static int heartRateDevice(Context c) {
        return prefs(c).getInt(KEY_HR_DEVICE, 0);
    }

    public static void setHeartRateDevice(Context c, int deviceNumber) {
        prefs(c).edit().putInt(KEY_HR_DEVICE, deviceNumber).apply();
        BtLog.log("setting: heart-rate monitor " + (deviceNumber == 0 ? "forgotten" : deviceNumber));
    }
}
