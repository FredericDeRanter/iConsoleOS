package eu.le_tian.iConsoleOS.communication;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IdlePolicyTest {
    private static final long MIN = 60_000L;

    @Test
    public void idleOnlyAfterTheConfiguredTime() {
        assertFalse(IdlePolicy.isIdle(10 * MIN - 1, 10));
        assertTrue(IdlePolicy.isIdle(10 * MIN, 10));
        assertFalse("0 = never", IdlePolicy.isIdle(1000 * MIN, 0));
    }

    @Test
    public void pedallingAndCommandsCountAsActivity() {
        IdlePolicy p = new IdlePolicy();
        p.reset(0);
        p.onData(0, 0, 5 * MIN);           // standing still: not activity
        assertEquals(6 * MIN, p.idleMs(6 * MIN));
        p.onData(55, 0, 7 * MIN);          // pedalling
        assertEquals(MIN, p.idleMs(8 * MIN));
        p.onData(0, 3, 9 * MIN);           // wheel still turning
        assertEquals(0, p.idleMs(9 * MIN));
        p.onUserCommand(20 * MIN);         // level change, start, stop
        assertEquals(MIN, p.idleMs(21 * MIN));
    }

    @Test
    public void forgottenWorkoutBecomesIdle() {
        // A workout left running: the bike keeps sending zero samples.
        IdlePolicy p = new IdlePolicy();
        p.reset(0);
        p.onData(70, 250, MIN);
        for (long t = 2 * MIN; t <= 15 * MIN; t += 500)
            p.onData(0, 0, t);
        assertTrue(IdlePolicy.isIdle(p.idleMs(15 * MIN), 10));
    }
}
