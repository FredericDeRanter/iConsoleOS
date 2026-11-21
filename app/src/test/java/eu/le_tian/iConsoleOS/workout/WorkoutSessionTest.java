package eu.le_tian.iConsoleOS.workout;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.le_tian.iConsoleOS.workout.ProfileTimeline.Step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WorkoutSessionTest {

    /** Records the levels sent; can refuse them like a disconnected bike. */
    private final List<Integer> sent = new ArrayList<>();
    private boolean bikeAccepts = true;
    private WorkoutSession session;

    @Before
    public void setUp() {
        session = new WorkoutSession(level -> {
            if (bikeAccepts)
                sent.add(level);
            return bikeAccepts;
        });
    }

    private static List<Step> steps(int... secondLevel) {
        List<Step> l = new ArrayList<>();
        for (int i = 0; i < secondLevel.length; i += 2)
            l.add(new Step(secondLevel[i], secondLevel[i + 1]));
        return l;
    }

    /** 2 min L5, 5 min L8, 40 s L28, 2 min L4. */
    private void intervalProfile() {
        session.setProfile(steps(0, 5, 120, 8, 420, 28, 460, 4));
        session.setDuration(580);
    }

    @Test
    public void noRecordingOutsideARide() {
        intervalProfile();
        assertNull(session.onSample(5));
        assertEquals(580, session.idleDisplaySeconds());
    }

    @Test
    public void quickStartCountsUpAndSetsTheStartLevel() {
        session.setProfile(steps(0, 3));
        session.setDuration(0);
        session.start();
        WorkoutSession.Tick tick = session.onSample(1);
        assertEquals(1, tick.rideSeconds);
        assertEquals(1, tick.displaySeconds);
        assertEquals(Collections.singletonList(3), sent);
        session.onSample(2);
        assertEquals("start level only in the first second", 1, sent.size());

        assertTrue(session.requestLevel(5)); // + + at 10 s
        assertEquals(2, session.extraLevel());
        session.onSample(10);
        session.onSample(11);
        assertEquals(steps(2, 3, 11, 5), session.done());
    }

    @Test
    public void profileStepsLevelsAndCountsDown() {
        intervalProfile();
        session.start();
        assertEquals(579, session.onSample(1).displaySeconds);
        assertEquals(Collections.singletonList(5), sent);

        session.onSample(119);
        assertEquals(1, sent.size());
        session.onSample(120);
        assertEquals(Arrays.asList(5, 8), sent);
        assertEquals(8, session.currentLevel());

        // The user adds 2 during the 5-minute step: kept for the rest of the profile.
        assertTrue(session.requestLevel(10));
        assertEquals(2, session.extraLevel());
        session.onSample(130);
        assertEquals(160, session.onSample(420).displaySeconds);
        assertEquals(30, session.currentLevel());
        session.onSample(460);
        assertEquals(Arrays.asList(5, 8, 10, 30, 6), sent);
        // The ridden line: one step per level, extended to the last sample of that level.
        assertEquals(steps(119, 5, 120, 8, 130, 10, 420, 30, 460, 6), session.done());
    }

    @Test
    public void afterTheProfileItCountsUpAndKeepsTheLevel() {
        intervalProfile();
        session.start();
        for (int t : new int[]{1, 120, 420, 460})
            session.onSample(t);
        int sentBefore = sent.size();
        WorkoutSession.Tick tick = session.onSample(600);
        assertEquals("counts up after the end", 600, tick.displaySeconds);
        assertEquals(sentBefore, sent.size());
        assertEquals(4, session.done().get(session.done().size() - 1).level);
    }

    @Test
    public void pausedSecondsAreLeftOutOfTheRideTime() {
        session.setProfile(steps(0, 3));
        session.start();
        assertEquals(10, session.onSample(10).rideSeconds);
        session.pause();
        for (long t = 11; t <= 20; t++)
            assertNull("not recorded while paused", session.onSample(t));
        session.resume();
        assertEquals(11, session.onSample(21).rideSeconds);
    }

    @Test
    public void veryShortPauseDoesNotMakeTheTimeJump() {
        session.setProfile(steps(0, 3));
        session.start();
        session.onSample(100);
        session.pause();
        session.onSample(101); // a single paused sample
        session.resume();
        assertEquals(101, session.onSample(102).rideSeconds);
    }

    @Test
    public void pauseWithoutSamplesChangesNothing() {
        session.setProfile(steps(0, 3));
        session.start();
        session.pause();
        session.resume();
        assertEquals(50, session.onSample(50).rideSeconds);
    }

    @Test
    public void firstSamplePastTheEndOfTheProfileDoesNotCrash() {
        // E.g. paused through the whole profile: the ridden line is still empty (the old code
        // read its last point and crashed).
        session.setProfile(steps(0, 5, 60, 8));
        session.setDuration(60);
        session.start();
        assertEquals(100, session.onSample(100).displaySeconds);
        assertEquals(steps(100, 5), session.done());
    }

    @Test
    public void levelsAreClampedAndRefusedLevelsChangeNothing() {
        session.setProfile(steps(0, 3));
        assertTrue(session.requestLevel(40));
        assertEquals(32, session.currentLevel());
        assertTrue(session.requestLevel(-3));
        assertEquals(1, session.currentLevel());

        bikeAccepts = false;
        assertFalse(session.requestLevel(10));
        assertEquals(1, session.currentLevel());
    }

    @Test
    public void profileCantChangeDuringARide() {
        intervalProfile();
        session.start();
        session.setProfile(steps(0, 20));
        session.setDuration(0);
        assertEquals(580, session.duration());
        session.onSample(1);
        assertEquals(Collections.singletonList(5), sent);
    }

    @Test
    public void resetGoesBackToTheStartState() {
        intervalProfile();
        session.start();
        session.onSample(1);
        session.requestLevel(9);
        session.stop();
        assertFalse(session.isRunning());
        assertNull(session.onSample(2));
        session.reset();
        assertEquals(1, session.currentLevel());
        assertEquals(0, session.extraLevel());
        assertTrue(session.done().isEmpty());
    }
}
