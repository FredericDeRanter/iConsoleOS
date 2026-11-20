package eu.le_tian.iConsoleOS.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WorkoutSummaryTest {

    private static ExerciseData sample(long t, int power, int speed, int dist, int cal, int hr, int rpm) {
        return new ExerciseData(0, power, speed, dist, cal, hr, rpm, t, 1);
    }

    @Test
    public void noSamplesGivesNoSummary() {
        assertNull(WorkoutSummary.of(null));
        assertNull(WorkoutSummary.of(Collections.emptyList()));
    }

    @Test
    public void averagesMaximaAndTotals() {
        WorkoutSummary s = WorkoutSummary.of(Arrays.asList(
                sample(0, 1000, 200, 0, 0, 90, 60),
                sample(1, 2000, 300, 5, 1, 120, 80),
                sample(2, 1500, 250, 9, 2, 110, 70)));
        assertEquals(2, s.totalDuration);
        assertEquals(9, s.totalDistance);
        assertEquals(2, s.totalCalories);
        assertEquals(1500, s.avgPower);
        assertEquals(2000, s.maxPower);
        assertEquals(250, s.avgSpeed);
        assertEquals(300, s.maxSpeed);
        assertEquals(106, s.avgHR);   // 320 / 3, integer division like before
        assertEquals(120, s.maxHR);
        assertEquals(70, s.avgRPM);
        assertEquals(80, s.maxRPM);
    }

    @Test
    public void longRideDoesNotOverflow() {
        // 24 h at 2 samples/s with high power: the int sum used before would overflow.
        List<ExerciseData> samples = new ArrayList<>();
        for (int i = 0; i < 2 * 24 * 3600; i++)
            samples.add(sample(i / 2, 30000, 400, 0, 0, 100, 90));
        WorkoutSummary s = WorkoutSummary.of(samples);
        assertEquals(30000, s.avgPower);
        assertTrue(s.totalDuration > 0);
    }

    @Test
    public void applyToFillsExercise() {
        Exercise e = new Exercise(1, 1000, 0, "Quick Start", 1);
        WorkoutSummary.of(Collections.singletonList(sample(60, 900, 210, 3, 4, 100, 75))).applyTo(e);
        assertEquals(60, e.getTotalDuration());
        assertEquals(900, e.getAvgPower());
        assertEquals(75, e.getMaxRPM());
        assertEquals(3, e.getTotalDistance());
    }
}
