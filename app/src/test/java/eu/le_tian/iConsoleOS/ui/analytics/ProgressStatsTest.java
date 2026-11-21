package eu.le_tian.iConsoleOS.ui.analytics;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import eu.le_tian.iConsoleOS.data.Exercise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ProgressStatsTest {
    private static final TimeZone BRUSSELS = TimeZone.getTimeZone("Europe/Brussels");
    private static final double EPS = 1e-9;

    private static long at(int year, int month, int day, int hour) {
        Calendar c = Calendar.getInstance(BRUSSELS);
        c.clear();
        c.set(year, month - 1, day, hour, 0, 0);
        return c.getTimeInMillis();
    }

    /** A summarized ride; power in W, duration in minutes. */
    private static Exercise ride(long start, int minutes, double avgPowerW, double npW) {
        Exercise e = new Exercise(0, start, start + minutes * 60_000L, "Quick Start", 1);
        e.setSummarized(true);
        e.setTotalDuration(minutes * 60L);
        e.setAvgPower((int) Math.round(avgPowerW * 10));
        e.setNormPower((int) Math.round(npW * 10));
        e.setTotalDistance(minutes * 4);   // 24 km/h
        e.setAvgSpeed(240);
        e.setAvgRPM(70);
        e.setTotalCalories(minutes * 10);
        return e;
    }

    @Test
    public void weeksAroundNewYearUseTheIsoWeekYear() {
        // Mon 29 Dec 2025 - Sun 4 Jan 2026 is ISO week 1 of 2026.
        Calendar c = ProgressStats.calendar(BRUSSELS);
        long dec29 = ProgressStats.periodStart(at(2025, 12, 31, 19), false, c);
        assertEquals(at(2025, 12, 29, 0), dec29);
        assertEquals("W01 '26", ProgressStats.label(dec29, false, c));
        assertEquals(dec29, ProgressStats.periodStart(at(2026, 1, 4, 20), false, c));
        // Fri 1 Jan 2027 belongs to week 53 of 2026.
        long dec28 = ProgressStats.periodStart(at(2027, 1, 1, 10), false, c);
        assertEquals(at(2026, 12, 28, 0), dec28);
        assertEquals("W53 '26", ProgressStats.label(dec28, false, c));
    }

    @Test
    public void ridesAcrossNewYearLandInSeparateWeeks() {
        // The old "yyyy-'W'ww" key put 30 Dec 2024 (W01 '25) together with 2 Jan 2024's week 1.
        List<Exercise> rides = Arrays.asList(
                ride(at(2024, 1, 2, 19), 30, 100, 0),
                ride(at(2024, 12, 30, 19), 40, 110, 0),
                ride(at(2025, 1, 2, 19), 50, 120, 0));
        List<ProgressStats.Period> weeks = ProgressStats.periods(rides, 0, at(2025, 1, 5, 23), false, BRUSSELS);
        ProgressStats.Period first = weeks.get(0);
        ProgressStats.Period last = weeks.get(weeks.size() - 1);
        assertEquals("W01 '24", first.label);
        assertEquals(1, first.rides);
        assertEquals("W01 '25", last.label);
        assertEquals(2, last.rides);
        assertEquals(53, weeks.size()); // 2024 has 52 ISO weeks, plus W01 '25
    }

    @Test
    public void periodsIncludeEmptyOnesAndMonthLabels() {
        List<Exercise> rides = Arrays.asList(
                ride(at(2026, 9, 10, 19), 30, 100, 0),
                ride(at(2026, 11, 20, 19), 30, 100, 0));
        List<ProgressStats.Period> months = ProgressStats.periods(rides, at(2026, 9, 1, 0), at(2026, 11, 21, 21), true, BRUSSELS);
        assertEquals(3, months.size());
        assertEquals("Sep '26", months.get(0).label);
        assertEquals("Oct '26", months.get(1).label);
        assertEquals(0, months.get(1).rides);
        assertTrue(Double.isNaN(months.get(1).avgPower()));
        assertEquals(0, months.get(1).distanceKm, EPS);
        assertEquals(1, months.get(2).rides);
    }

    @Test
    public void weekStartsStayOnMondayAcrossDst() {
        List<ProgressStats.Period> weeks = ProgressStats.periods(Collections.emptyList(),
                at(2026, 3, 1, 12), at(2026, 11, 1, 12), false, BRUSSELS);
        Calendar c = Calendar.getInstance(BRUSSELS);
        for (ProgressStats.Period p : weeks) {
            c.setTimeInMillis(p.startMillis);
            assertEquals(p.label, Calendar.MONDAY, c.get(Calendar.DAY_OF_WEEK));
            assertEquals(p.label, 0, c.get(Calendar.HOUR_OF_DAY));
        }
    }

    @Test
    public void averagesAreWeightedByDuration() {
        // 60 min at 200 W and 20 min at 100 W: (200*60 + 100*20) / 80 = 175 W, not 150 W.
        ProgressStats.Totals t = ProgressStats.totals(Arrays.asList(
                ride(at(2026, 11, 2, 19), 60, 200, 210),
                ride(at(2026, 11, 4, 19), 20, 100, 0)), 0, Long.MAX_VALUE);
        assertEquals(2, t.rides);
        assertEquals(175, t.avgPower(), EPS);
        assertEquals("NP only from rides that have it", 210, t.normPower(), EPS);
        assertTrue("no HR measured", Double.isNaN(t.avgHR()));
        assertEquals(80, t.minutes(), EPS);
        assertEquals(60, t.longestRideMin, EPS);
        // energy: 200 W * 3600 s + 100 W * 1200 s = 840 kJ
        assertEquals(840, t.energyKJ, EPS);
        assertEquals(840, t.value(ProgressStats.Metric.ENERGY), EPS);
        assertEquals(24, t.avgSpeed(), EPS);
    }

    @Test
    public void filterByTimeframeAndProfile() {
        Exercise a = ride(at(2026, 11, 2, 19), 30, 100, 0);
        Exercise b = ride(at(2026, 11, 5, 19), 30, 100, 0);
        b.setExProfileName("Interval training");
        Exercise unsummarized = new Exercise(0, at(2026, 11, 6, 19), 0, "Quick Start", 1);
        List<Exercise> rides = Arrays.asList(b, a, unsummarized);
        assertEquals(Arrays.asList(a, b), ProgressStats.filter(rides, 0, Long.MAX_VALUE, null));
        assertEquals(Collections.singletonList(b), ProgressStats.filter(rides, 0, Long.MAX_VALUE, "Interval training"));
        assertEquals(Collections.singletonList(a), ProgressStats.filter(rides, 0, at(2026, 11, 3, 0), null));
    }

    @Test
    public void trendSkipsEmptyAveragePeriods() {
        double nan = Double.NaN;
        double[] t = ProgressStats.trend(new double[]{100, nan, 200, 300, 400, nan, nan, nan, nan}, 4);
        assertEquals(100, t[0], EPS);
        assertEquals(100, t[1], EPS);
        assertEquals(150, t[2], EPS);
        assertEquals(200, t[3], EPS);
        assertEquals(300, t[4], EPS);         // 200, 300, 400 (+ NaN)
        assertEquals(350, t[6], EPS);         // 400 only... and 300
        assertEquals(400, t[7], EPS);
        assertTrue(Double.isNaN(t[8]));
    }

    @Test
    public void changeVersusPrevious() {
        assertEquals(0.12, ProgressStats.change(112, 100), EPS);
        assertTrue(Double.isNaN(ProgressStats.change(112, 0)));
        assertTrue(Double.isNaN(ProgressStats.change(Double.NaN, 100)));
    }

    @Test
    public void personalBests() {
        Exercise a = ride(at(2026, 11, 2, 19), 30, 100, 0);
        a.setBest5min(1500);
        a.setBest20min(1200);
        Exercise b = ride(at(2026, 11, 5, 19), 30, 100, 0);
        b.setBest5min(1400);
        b.setBest20min(1300);
        List<Exercise> rides = Arrays.asList(a, b);
        assertSame(a, ProgressStats.best(rides, ProgressStats.BestEffort.MIN_5));
        assertSame(b, ProgressStats.best(rides, ProgressStats.BestEffort.MIN_20));
        assertNull(ProgressStats.best(rides, ProgressStats.BestEffort.MIN_60));
    }
}
