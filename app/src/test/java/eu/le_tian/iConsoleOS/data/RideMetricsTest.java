package eu.le_tian.iConsoleOS.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RideMetricsTest {

    private static ExerciseData sample(long t, int power10) {
        return sample(t, power10, 0);
    }

    private static ExerciseData sample(long t, int power10, int hr) {
        return new ExerciseData(0, power10, 250, 0, 0, hr, 70, t, 1);
    }

    /** One sample per second, power10[i] at second i. */
    private static List<ExerciseData> series(int... power10) {
        List<ExerciseData> l = new ArrayList<>();
        for (int i = 0; i < power10.length; i++)
            l.add(sample(i, power10[i]));
        return l;
    }

    private static List<ExerciseData> constant(int seconds, int power10) {
        int[] p = new int[seconds];
        Arrays.fill(p, power10);
        return series(p);
    }

    @Test
    public void noSamples() {
        assertNull(RideMetrics.of(null));
        assertNull(RideMetrics.of(Collections.emptyList()));
    }

    @Test
    public void averageWeighsEverySecondOnce() {
        // Second 0 has three samples at 100 W, second 1 one sample at 200 W.
        // Per sample: (100+100+100+200)/4 = 125 W; per second: (100+200)/2 = 150 W.
        RideMetrics m = RideMetrics.of(Arrays.asList(
                sample(0, 1000), sample(0, 1000), sample(0, 1000), sample(1, 2000)));
        assertEquals(2, m.seconds);
        assertEquals(1500, m.avgPower);
    }

    @Test
    public void gapHoldsPreviousValue() {
        // Seconds 1 and 2 missing (dropped packets): they keep 100 W, then 400 W at second 3.
        RideMetrics m = RideMetrics.of(Arrays.asList(sample(0, 1000), sample(3, 4000)));
        assertEquals(4, m.seconds);
        assertEquals((1000 * 3 + 4000) / 4, m.avgPower);
    }

    @Test
    public void outOfOrderSamplesAreFine() {
        RideMetrics a = RideMetrics.of(series(1000, 2000, 3000));
        RideMetrics b = RideMetrics.of(Arrays.asList(sample(2, 3000), sample(0, 1000), sample(1, 2000)));
        assertEquals(a.avgPower, b.avgPower);
        assertEquals(a.seconds, b.seconds);
    }

    @Test
    public void heartRateIgnoresSecondsWithoutHeartRate() {
        RideMetrics m = RideMetrics.of(Arrays.asList(
                sample(0, 1000, 0), sample(1, 1000, 120), sample(2, 1000, 0), sample(3, 1000, 130)));
        assertEquals(125, m.avgHR);
        assertEquals(0, RideMetrics.of(series(1000, 1000)).avgHR);
    }

    @Test
    public void normalizedPowerOfSteadyRideIsItsAverage() {
        RideMetrics m = RideMetrics.of(constant(600, 1500));
        assertEquals(1500, m.normPower);
        assertEquals(1500, m.avgPower);
    }

    @Test
    public void normalizedPowerOfIntervalsIsAboveAverage() {
        // 10 x (60 s at 300 W, 60 s at 100 W): average 200 W.
        int[] p = new int[1200];
        for (int i = 0; i < p.length; i++)
            p[i] = (i / 60) % 2 == 0 ? 3000 : 1000;
        RideMetrics m = RideMetrics.of(series(p));
        assertEquals(2000, m.avgPower);
        assertEquals(expectedNP(p), m.normPower);
        assertTrue("NP " + m.normPower, m.normPower > 2200 && m.normPower < 2600);
    }

    @Test
    public void normalizedPowerNeedsThirtySeconds() {
        assertEquals(0, RideMetrics.of(constant(29, 1500)).normPower);
        assertEquals(1500, RideMetrics.of(constant(30, 1500)).normPower);
    }

    @Test
    public void bestEffortsFindTheHardestWindow() {
        // 30 min at 100 W with 5 min at 250 W starting at minute 10.
        int[] p = new int[30 * 60];
        Arrays.fill(p, 1000);
        Arrays.fill(p, 600, 900, 2500);
        RideMetrics m = RideMetrics.of(series(p));
        assertEquals(2500, m.best5min);
        // 20 min: the window that includes the whole 5-minute effort.
        assertEquals((15 * 60 * 1000 + 5 * 60 * 2500) / (20 * 60), m.best20min);
        assertEquals("ride shorter than 60 min", 0, m.best60min);
    }

    @Test
    public void fillInSkipsFragmentsOfOlderRides() {
        Exercise e = new Exercise(1, 0, 0, "Quick Start", 1);
        e.setSummarized(true);
        e.setTotalDuration(2555);
        // Only one sample survived (older builds deleted the rest): no metrics, but marked done.
        assertTrue(RideMetrics.fillIn(e, Collections.singletonList(sample(2555, 1100))));
        assertEquals(0, e.getNormPower());
        assertEquals(RideMetrics.VERSION, e.getMetricsVersion());
        assertFalse("not retried", RideMetrics.fillIn(e, constant(2600, 1500)));
    }

    @Test
    public void fillInFullRide() {
        Exercise e = new Exercise(1, 0, 0, "Quick Start", 1);
        e.setTotalDuration(599);
        e.setAvgPower(1234); // stored average is never changed by fillIn
        assertTrue(RideMetrics.fillIn(e, constant(600, 1500)));
        assertEquals(1500, e.getNormPower());
        assertEquals(1500, e.getBest5min());
        assertEquals(0, e.getBest20min());
        assertEquals(1234, e.getAvgPower());
        assertEquals(RideMetrics.VERSION, e.getMetricsVersion());
    }

    /** Straightforward NP: 30 s rolling averages, 4th power, mean, 4th root. */
    private static int expectedNP(int[] p) {
        double sum4 = 0;
        int n = 0;
        for (int end = 30; end <= p.length; end++) {
            double s = 0;
            for (int i = end - 30; i < end; i++)
                s += p[i];
            sum4 += Math.pow(s / 30, 4);
            n++;
        }
        return (int) Math.round(Math.pow(sum4 / n, 0.25));
    }
}
