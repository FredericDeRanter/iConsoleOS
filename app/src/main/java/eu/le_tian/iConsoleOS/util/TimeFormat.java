package eu.le_tian.iConsoleOS.util;

import java.util.Locale;

/** Clock-style durations for the displays. Plain Java so it can be unit tested. */
public final class TimeFormat {
    private TimeFormat() {
    }

    /**
     * "mm:ss", "h:mm:ss" from one hour, "dd:hh:mm:ss" from one day (hours kept as "00" then,
     * so 1 day and 5 minutes doesn't read as 1 hour and 5 minutes). Negative values show as 00:00.
     */
    public static String clock(long seconds) {
        if (seconds < 0)
            seconds = 0;
        StringBuilder b = new StringBuilder();
        long day = seconds / (60 * 60 * 24);
        long hour = (seconds % (60 * 60 * 24)) / (60 * 60);
        if (day > 0)
            b.append(String.format(Locale.US, "%02d:%02d:", day, hour));
        else if (hour > 0)
            b.append(String.format(Locale.US, "%d:", hour));
        b.append(String.format(Locale.US, "%02d:%02d", (seconds % (60 * 60)) / 60, seconds % 60));
        return b.toString();
    }
}
