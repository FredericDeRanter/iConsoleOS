package eu.le_tian.iConsoleOS.workout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.le_tian.iConsoleOS.workout.ProfileTimeline.Step;

/**
 * The state of one ride, without any Android code so it can be unit tested: running/paused, the
 * paused time, where we are in the profile, the level and the user's offset from the profile, and
 * the level line ridden so far (for the graph). {@code HomeFragment} only shows it.
 *
 * <p>The bike's clock ({@code IConsole.Data.mTime}, seconds) keeps running while paused; ride time
 * is the bike's time minus the paused seconds. With a profile duration the display counts down,
 * otherwise (Quick Start) it counts up; after the profile's end it counts up again.
 *
 * <p>Levels: the profile says which level applies from which second. +/− and the slider set an
 * offset ({@link #extraLevel()}) that is kept for the rest of the profile. Levels are 1..32.
 */
public final class WorkoutSession {
    public static final int MIN_LEVEL = 1, MAX_LEVEL = 32;

    /** Sends a level to the bike; false when it couldn't (not connected, not in the reading state). */
    public interface Bike {
        boolean sendLevel(int level);
    }

    /** What to do with one bike sample. */
    public static final class Tick {
        /** Ride time in seconds (pauses excluded): the sample's timestamp. */
        public final long rideSeconds;
        /** Seconds to show: remaining time while a profile with a duration runs, else ride time. */
        public final long displaySeconds;

        Tick(long rideSeconds, long displaySeconds) {
            this.rideSeconds = rideSeconds;
            this.displaySeconds = displaySeconds;
        }
    }

    private final Bike mBike;
    private List<Step> mProfile = Collections.emptyList();
    private int mDuration;

    private boolean mRunning, mPaused;
    private long mPauseFirst, mPauseLast, mSecondsPaused;
    private int mProfileIndex;
    private int mExtraLevel;
    private int mCurrentLevel = MIN_LEVEL;
    private final List<Step> mDone = new ArrayList<>();

    public WorkoutSession(Bike bike) {
        mBike = bike;
    }

    // ---- profile ----

    /** The selected profile's level timeline ({@link ProfileTimeline#build}). Ignored during a ride. */
    public void setProfile(List<Step> timeline) {
        if (!mRunning)
            mProfile = timeline != null ? timeline : Collections.<Step>emptyList();
    }

    /** Profile duration in seconds, 0 = open-ended. Ignored during a ride. */
    public void setDuration(int seconds) {
        if (!mRunning)
            mDuration = seconds;
    }

    public int duration() {
        return mDuration;
    }

    // ---- start / pause / stop ----

    public void start() {
        mRunning = true;
        mPaused = false;
        mProfileIndex = 0;
        mSecondsPaused = 0;
        mPauseFirst = mPauseLast = 0;
        mExtraLevel = 0;
        mDone.clear();
    }

    public void pause() {
        if (mRunning)
            mPaused = true;
    }

    /** Continue after a pause: the seconds the bike counted while paused are left out of the ride time. */
    public void resume() {
        if (!mPaused)
            return;
        if (mPauseFirst != 0) {
            // Paused from the first to the last paused sample, +1 for the second in between.
            // (Before: a pause with a single sample gave "0 - first" and the time jumped.)
            long last = mPauseLast != 0 ? mPauseLast : mPauseFirst;
            mSecondsPaused += last - mPauseFirst + 1;
        }
        mPaused = false;
        mPauseFirst = mPauseLast = 0;
    }

    /** End of the ride; {@link #reset} once the summary was shown. */
    public void stop() {
        mRunning = false;
    }

    /** Back to the state before a ride (level 1, nothing ridden). */
    public void reset() {
        mRunning = false;
        mPaused = false;
        mProfileIndex = 0;
        mExtraLevel = 0;
        mSecondsPaused = 0;
        mPauseFirst = mPauseLast = 0;
        mCurrentLevel = MIN_LEVEL;
        mDone.clear();
    }

    public boolean isRunning() {
        return mRunning;
    }

    public boolean isPaused() {
        return mPaused;
    }

    // ---- levels ----

    /**
     * Asks the bike for a level (clamped to 1..32). When the bike takes it, it becomes the current
     * level and the offset from the profile is updated.
     *
     * @return whether the bike took it
     */
    public boolean requestLevel(int level) {
        level = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        if (!mBike.sendLevel(level))
            return false;
        mCurrentLevel = level;
        if (!mProfile.isEmpty())
            mExtraLevel = level - mProfile.get(Math.min(mProfileIndex, mProfile.size() - 1)).level;
        return true;
    }

    /** The level shown when nothing was sent yet (a new connection, after Stop). */
    public void resetLevel() {
        mCurrentLevel = MIN_LEVEL;
    }

    public int currentLevel() {
        return mCurrentLevel;
    }

    /** The user's offset from the profile's levels (+/− and slider). */
    public int extraLevel() {
        return mExtraLevel;
    }

    /** The level line ridden so far: one step per level change, the last one up to now. */
    public List<Step> done() {
        return Collections.unmodifiableList(mDone);
    }

    // ---- samples ----

    /**
     * Handles one bike sample during a ride.
     *
     * @return what to record and show, or null when it must not be recorded (no ride, or paused)
     */
    public Tick onSample(long bikeSeconds) {
        if (!mRunning)
            return null;
        if (mPaused) {
            if (mPauseFirst == 0)
                mPauseFirst = bikeSeconds;
            else
                mPauseLast = bikeSeconds;
            return null;
        }
        long t = bikeSeconds - mSecondsPaused;
        return new Tick(t, followProfile(t));
    }

    /** The time to show before a ride: the profile's duration, or 0:00. Clears the ridden line. */
    public long idleDisplaySeconds() {
        mDone.clear();
        return mDuration;
    }

    /** Level stepping and the ridden line for ride second {@code t}; returns the seconds to show. */
    private long followProfile(long t) {
        if (mProfile.isEmpty())
            return t; // no timeline yet: just count
        if (t <= 0) {
            mDone.clear(); // not started yet
            return mDuration;
        }

        if (mDuration == 0) { // open-ended (Quick Start)
            int level = mExtraLevel + mProfile.get(0).level;
            if (t < 2)
                requestLevel(level); // the start level
            addDone(t, level);
            return t;
        }

        long left = mDuration - t;
        if (left <= 0) { // past the profile's end: keep riding, count up, keep the last level
            addDone(t, mProfile.get(Math.min(mProfileIndex, mProfile.size() - 1)).level + mExtraLevel);
            return t;
        }

        if (mProfileIndex >= mProfile.size())
            mProfileIndex = mProfile.size() - 1; // can't happen; be safe
        int level = mProfile.get(mProfileIndex).level + mExtraLevel;
        if (mProfileIndex < mProfile.size() - 1 && t >= mProfile.get(mProfileIndex + 1).second) {
            mProfileIndex++;
            level = mProfile.get(mProfileIndex).level + mExtraLevel;
            requestLevel(level);
        }
        if (mProfileIndex == 0 && t < 2)
            requestLevel(level); // the start level
        addDone(t, level);
        return left;
    }

    /** Extends the ridden line to second {@code t}, with a new step when the level changed. */
    private void addDone(long t, int level) {
        Step now = new Step((int) t, level);
        int last = mDone.size() - 1;
        if (last >= 0 && mDone.get(last).level == level)
            mDone.set(last, now);
        else
            mDone.add(now);
    }
}
