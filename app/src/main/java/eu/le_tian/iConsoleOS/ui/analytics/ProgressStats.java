package eu.le_tian.iConsoleOS.ui.analytics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import eu.le_tian.iConsoleOS.data.Exercise;

/**
 * Groups rides into weeks or months and computes the numbers of the Progress screen. Plain Java
 * (no Android classes) so it can be unit tested.
 *
 * <ul>
 *   <li>Weeks are ISO weeks: Monday to Sunday, numbered within the week-based year, so the days
 *   around New Year land in the right week (Mon 29 Dec 2025 is week 1 of 2026).</li>
 *   <li>Averages over several rides are weighted by ride duration: a 60-minute ride counts four
 *   times as much as a 15-minute one, like one long ride would.</li>
 *   <li>NP and heart rate only average the rides that have them (0 = unknown).</li>
 * </ul>
 */
public final class ProgressStats {

    /** Number of periods in the trend line's rolling average. */
    public static final int TREND_PERIODS = 4;

    private ProgressStats() {
    }

    /** Totals and duration-weighted averages of a set of rides. */
    public static class Totals {
        public int rides;
        public long seconds;
        public double distanceKm, calories, energyKJ;
        public double longestRideMin;

        // Σ(average × seconds) and Σ seconds of the rides that have the value
        private double powerSum, npSum, speedSum, rpmSum, hrSum;
        private long powerSec, npSec, speedSec, rpmSec, hrSec;

        void add(Exercise e) {
            rides++;
            long s = e.getTotalDuration();
            seconds += s;
            distanceKm += e.getTotalDistance() / 10.0;
            calories += e.getTotalCalories();
            energyKJ += e.getAvgPower() / 10.0 * s / 1000.0;
            longestRideMin = Math.max(longestRideMin, s / 60.0);
            if (s <= 0)
                return;
            powerSum += e.getAvgPower() / 10.0 * s;
            powerSec += s;
            speedSum += e.getAvgSpeed() / 10.0 * s;
            speedSec += s;
            rpmSum += e.getAvgRPM() * (double) s;
            rpmSec += s;
            if (e.getNormPower() > 0) {
                npSum += e.getNormPower() / 10.0 * s;
                npSec += s;
            }
            if (e.getAvgHR() > 0) {
                hrSum += e.getAvgHR() * (double) s;
                hrSec += s;
            }
        }

        public double minutes() {
            return seconds / 60.0;
        }

        /** Watts; NaN when there are no rides. */
        public double avgPower() {
            return ratio(powerSum, powerSec);
        }

        /** Watts; NaN when no ride has NP. */
        public double normPower() {
            return ratio(npSum, npSec);
        }

        /** km/h */
        public double avgSpeed() {
            return ratio(speedSum, speedSec);
        }

        public double avgRPM() {
            return ratio(rpmSum, rpmSec);
        }

        /** bpm; NaN when no ride has a heart rate. */
        public double avgHR() {
            return ratio(hrSum, hrSec);
        }

        private static double ratio(double sum, long weight) {
            return weight > 0 ? sum / weight : Double.NaN;
        }

        public double value(Metric m) {
            switch (m) {
                case DISTANCE: return distanceKm;
                case DURATION: return minutes();
                case ENERGY: return energyKJ;
                case CALORIES: return calories;
                case RIDES: return rides;
                case AVG_POWER: return avgPower();
                case NORM_POWER: return normPower();
                case AVG_SPEED: return avgSpeed();
                case AVG_RPM: return avgRPM();
                case AVG_HR: return avgHR();
                default: throw new IllegalArgumentException(m.toString());
            }
        }
    }

    /** One week or month. */
    public static final class Period extends Totals {
        public final long startMillis;
        public final String label;

        Period(long startMillis, String label) {
            this.startMillis = startMillis;
            this.label = label;
        }
    }

    public enum Metric {
        DISTANCE("Distance", "km", true),
        DURATION("Duration", "min", true),
        ENERGY("Energy", "kJ", true),
        CALORIES("Calories", "kcal", true),
        RIDES("Workouts", "rides", true),
        AVG_POWER("Avg power", "W", false),
        NORM_POWER("Normalized Power", "W", false),
        AVG_SPEED("Avg speed", "km/h", false),
        AVG_RPM("Avg cadence", "rpm", false),
        AVG_HR("Avg heart rate", "bpm", false);

        public final String title, unit;
        /** Totals add up over a period (bars); averages don't (an empty period has no value). */
        public final boolean total;

        Metric(String title, String unit, boolean total) {
            this.title = title;
            this.unit = unit;
            this.total = total;
        }
    }

    /** ISO-week calendar: weeks start on Monday, week 1 has the year's first Thursday. */
    static Calendar calendar(TimeZone tz) {
        Calendar c = Calendar.getInstance(tz, Locale.US);
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.setMinimalDaysInFirstWeek(4);
        return c;
    }

    /** Start (00:00 local time) of the week or month containing {@code millis}. */
    static long periodStart(long millis, boolean perMonth, Calendar c) {
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (perMonth) {
            c.set(Calendar.DAY_OF_MONTH, 1);
        } else {
            int back = (c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
            c.add(Calendar.DAY_OF_MONTH, -back);
        }
        return c.getTimeInMillis();
    }

    /** "W01 '26" or "Jan '26" for the period starting at {@code start}. */
    static String label(long start, boolean perMonth, Calendar c) {
        c.setTimeInMillis(start);
        if (perMonth) {
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            return String.format(Locale.US, "%s '%02d", months[c.get(Calendar.MONTH)], c.get(Calendar.YEAR) % 100);
        }
        // Thursday of this ISO week decides the week-based year
        c.add(Calendar.DAY_OF_MONTH, 3);
        return String.format(Locale.US, "W%02d '%02d", c.get(Calendar.WEEK_OF_YEAR), c.getWeekYear() % 100);
    }

    /**
     * Every week or month from the one containing {@code from} up to the one containing
     * {@code to}, including empty ones, with the rides that started in it.
     *
     * @param from start of the timeframe, or 0 for "since the first ride"
     */
    public static List<Period> periods(List<Exercise> rides, long from, long to, boolean perMonth, TimeZone tz) {
        Calendar c = calendar(tz);
        List<Exercise> inRange = filter(rides, from, to, null);
        if (from <= 0) {
            if (inRange.isEmpty())
                return new ArrayList<>();
            from = Long.MAX_VALUE;
            for (Exercise e : inRange)
                from = Math.min(from, e.getExStartDateTime());
        }

        List<Period> periods = new ArrayList<>();
        long last = periodStart(to, perMonth, c);
        for (long start = periodStart(from, perMonth, c); start <= last; ) {
            periods.add(new Period(start, label(start, perMonth, c)));
            c.setTimeInMillis(start);
            c.add(perMonth ? Calendar.MONTH : Calendar.WEEK_OF_YEAR, 1);
            start = periodStart(c.getTimeInMillis(), perMonth, c); // stays on Monday/1st across DST
        }

        for (Exercise e : inRange) {
            long start = periodStart(e.getExStartDateTime(), perMonth, c);
            // periods are sorted: binary search for the one starting at `start`
            int lo = 0, hi = periods.size() - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                long s = periods.get(mid).startMillis;
                if (s < start) lo = mid + 1;
                else if (s > start) hi = mid - 1;
                else {
                    periods.get(mid).add(e);
                    break;
                }
            }
        }
        return periods;
    }

    /** Totals over the rides that started in [from, to]. */
    public static Totals totals(List<Exercise> rides, long from, long to) {
        Totals t = new Totals();
        for (Exercise e : filter(rides, from, to, null))
            t.add(e);
        return t;
    }

    /**
     * Rides that started in [from, to], oldest first, optionally only one profile. Rides with
     * no start time or no duration (never summarized) are left out.
     */
    public static List<Exercise> filter(List<Exercise> rides, long from, long to, String profile) {
        List<Exercise> out = new ArrayList<>();
        for (Exercise e : rides) {
            long t = e.getExStartDateTime();
            if (t <= 0 || t < from || t > to || e.getTotalDuration() <= 0)
                continue;
            if (profile != null && !profile.equals(e.getExProfileName()))
                continue;
            out.add(e);
        }
        out.sort((a, b) -> Long.compare(a.getExStartDateTime(), b.getExStartDateTime()));
        return out;
    }

    /** The values of one metric per period; NaN for an average in a period without rides. */
    public static double[] values(List<Period> periods, Metric m) {
        double[] v = new double[periods.size()];
        for (int i = 0; i < v.length; i++)
            v[i] = periods.get(i).value(m);
        return v;
    }

    /**
     * Rolling mean over the last {@code window} periods (fewer at the start). NaN values (an
     * average in an empty period) are skipped; NaN when the whole window is empty.
     */
    public static double[] trend(double[] values, int window) {
        double[] out = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            double sum = 0;
            int n = 0;
            for (int j = Math.max(0, i - window + 1); j <= i; j++) {
                if (!Double.isNaN(values[j])) {
                    sum += values[j];
                    n++;
                }
            }
            out[i] = n > 0 ? sum / n : Double.NaN;
        }
        return out;
    }

    /** Relative change from {@code before} to {@code now}, e.g. 0.12 for +12%; NaN if unknown. */
    public static double change(double now, double before) {
        if (Double.isNaN(now) || Double.isNaN(before) || before == 0)
            return Double.NaN;
        return (now - before) / before;
    }

    /** The ride with the highest value of a best-effort column, or null when none has one. */
    public static Exercise best(List<Exercise> rides, BestEffort which) {
        Exercise best = null;
        for (Exercise e : rides) {
            if (which.of(e) > 0 && (best == null || which.of(e) > which.of(best)))
                best = e;
        }
        return best;
    }

    public enum BestEffort {
        MIN_5("5 min"), MIN_20("20 min"), MIN_60("60 min"), NP("NP");

        public final String title;

        BestEffort(String title) {
            this.title = title;
        }

        /** Power ×10, 0 = unknown. */
        public int of(Exercise e) {
            switch (this) {
                case MIN_5: return e.getBest5min();
                case MIN_20: return e.getBest20min();
                case MIN_60: return e.getBest60min();
                default: return e.getNormPower();
            }
        }
    }
}
