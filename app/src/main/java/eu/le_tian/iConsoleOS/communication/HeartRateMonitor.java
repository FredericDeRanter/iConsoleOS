package eu.le_tian.iConsoleOS.communication;

/**
 * Latest heart rate received over ANT+, and whether it is still fresh. Plain Java so it can be
 * unit tested; {@link HeartRateChannelController} feeds it the received data pages.
 *
 * <p>ANT+ heart-rate pages are 8 bytes; byte 7 is the "computed heart rate" in bpm on every page
 * type (0 = invalid). A value counts as fresh for {@link #MAX_AGE_MS}: the monitor sends about 4
 * pages per second, so 5 s without one means the watch stopped broadcasting or is out of range.
 */
public final class HeartRateMonitor {
    public static final long MAX_AGE_MS = 5000;

    private int mBpm;
    private long mReceivedAt;
    private boolean mHasValue;

    /** The heart rate in an ANT+ HR data page, 0 if invalid. */
    public static int computedHeartRate(byte[] page) {
        if (page == null || page.length < 8)
            return 0;
        return page[7] & 0xFF;
    }

    /** @return whether the page carried a valid heart rate */
    public synchronized boolean onPage(byte[] page, long nowMillis) {
        int bpm = computedHeartRate(page);
        if (bpm <= 0)
            return false;
        mBpm = bpm;
        mReceivedAt = nowMillis;
        mHasValue = true;
        return true;
    }

    /** Heart rate in bpm if it was received at most {@link #MAX_AGE_MS} ago, else 0. */
    public synchronized int heartRate(long nowMillis) {
        if (!mHasValue || nowMillis - mReceivedAt > MAX_AGE_MS || nowMillis < mReceivedAt)
            return 0;
        return mBpm;
    }

    public synchronized void clear() {
        mHasValue = false;
        mBpm = 0;
    }

    /**
     * Which heart rate to use: the watch's when it is fresh, otherwise the bike's (chest strap
     * or hand grips; 0 when it has none).
     */
    public static int choose(int watchBpm, int bikeBpm) {
        return watchBpm > 0 ? watchBpm : bikeBpm;
    }
}
