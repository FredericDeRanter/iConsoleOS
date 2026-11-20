package eu.le_tian.iConsoleOS.communication;

/**
 * Decides when a connected bike counts as idle: no pedalling and no command (start, stop, level)
 * for the configured time. Pure Java, thread-safe enough for one writer per event type.
 */
final class IdlePolicy {
    private volatile long mLastActivity;

    void reset(long now) {
        mLastActivity = now;
    }

    void onUserCommand(long now) {
        mLastActivity = now;
    }

    /** Called for every sample from the bike; only movement counts as activity. */
    void onData(int rpm, int speed10, long now) {
        if (rpm > 0 || speed10 > 0)
            mLastActivity = now;
    }

    long idleMs(long now) {
        return now - mLastActivity;
    }

    /** @param idleMinutes configured limit, 0 = never disconnect */
    static boolean isIdle(long idleMs, int idleMinutes) {
        return idleMinutes > 0 && idleMs >= idleMinutes * 60_000L;
    }
}
