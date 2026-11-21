package eu.le_tian.iConsoleOS.data;

import java.util.List;

/**
 * Time-based metrics of one ride, computed from its samples. Plain Java so it can be unit tested.
 * Units follow {@link ExerciseData}: power and speed are ×10.
 *
 * <p>The bike sends about two samples per second and the ride clock ({@code exTimestamp},
 * seconds, pauses excluded) has a one-second resolution. Samples are therefore first turned into
 * a one-value-per-second series: the samples of the same second are averaged, and a second
 * without samples (a dropped packet) holds the previous value. Averages over that series weigh
 * every second of the ride equally, however many samples it had.
 *
 * <ul>
 *   <li><b>Normalized Power</b>: 30 s rolling average of the power series, each value to the
 *   4th power, averaged, 4th root. Rides shorter than 30 s have none (0).</li>
 *   <li><b>Best efforts</b>: the highest average power over any 5/20/60 minutes of the ride
 *   (0 when the ride is shorter than the window).</li>
 *   <li><b>Average heart rate</b> only counts seconds with a heart rate (the bike reports 0
 *   without a chest strap).</li>
 * </ul>
 *
 * <p>Bump {@link #VERSION} when a calculation changes, so stored metrics are recomputed for the
 * rides that still have their samples.
 */
public final class RideMetrics {
    public static final int VERSION = 1;

    public static final int NP_WINDOW = 30;
    public static final int BEST_5 = 5 * 60, BEST_20 = 20 * 60, BEST_60 = 60 * 60;

    /** Number of one-second values: the ride's duration as covered by samples. */
    public final int seconds;
    public final int avgPower, avgSpeed, avgRPM, avgHR;
    public final int normPower;
    public final int best5min, best20min, best60min;

    private RideMetrics(int seconds, int avgPower, int avgSpeed, int avgRPM, int avgHR,
                        int normPower, int best5min, int best20min, int best60min) {
        this.seconds = seconds;
        this.avgPower = avgPower;
        this.avgSpeed = avgSpeed;
        this.avgRPM = avgRPM;
        this.avgHR = avgHR;
        this.normPower = normPower;
        this.best5min = best5min;
        this.best20min = best20min;
        this.best60min = best60min;
    }

    /** @return the metrics, or null when there are no samples */
    public static RideMetrics of(List<ExerciseData> samples) {
        if (samples == null || samples.isEmpty())
            return null;

        long first = Long.MAX_VALUE, last = Long.MIN_VALUE;
        for (ExerciseData d : samples) {
            first = Math.min(first, d.getExTimestamp());
            last = Math.max(last, d.getExTimestamp());
        }
        int n = (int) (last - first + 1);

        // Per-second sums and counts (samples can arrive out of timestamp order).
        long[] power = new long[n], speed = new long[n], rpm = new long[n], hr = new long[n];
        int[] count = new int[n], hrCount = new int[n];
        for (ExerciseData d : samples) {
            int i = (int) (d.getExTimestamp() - first);
            power[i] += d.getExPower();
            speed[i] += d.getExSpeed();
            rpm[i] += d.getExRPM();
            count[i]++;
            if (d.getExHR() > 0) {
                hr[i] += d.getExHR();
                hrCount[i]++;
            }
        }

        double[] p = new double[n], s = new double[n], c = new double[n], h = new double[n];
        boolean[] hasHR = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (count[i] > 0) {
                p[i] = (double) power[i] / count[i];
                s[i] = (double) speed[i] / count[i];
                c[i] = (double) rpm[i] / count[i];
            } else { // gap: hold the previous second (index 0 always has a sample)
                p[i] = p[i - 1];
                s[i] = s[i - 1];
                c[i] = c[i - 1];
            }
            if (hrCount[i] > 0) {
                h[i] = (double) hr[i] / hrCount[i];
                hasHR[i] = true;
            } else if (count[i] == 0 && i > 0) {
                h[i] = h[i - 1];
                hasHR[i] = hasHR[i - 1];
            }
        }

        double hrSum = 0;
        int hrSeconds = 0;
        for (int i = 0; i < n; i++) {
            if (hasHR[i]) {
                hrSum += h[i];
                hrSeconds++;
            }
        }

        return new RideMetrics(n, round(mean(p)), round(mean(s)), round(mean(c)),
                hrSeconds > 0 ? round(hrSum / hrSeconds) : 0,
                normalizedPower(p), bestAverage(p, BEST_5), bestAverage(p, BEST_20), bestAverage(p, BEST_60));
    }

    /** Normalized Power of a one-value-per-second power series; 0 if shorter than 30 s. */
    static int normalizedPower(double[] p) {
        if (p.length < NP_WINDOW)
            return 0;
        double window = 0, sum4 = 0;
        int values = 0;
        for (int i = 0; i < p.length; i++) {
            window += p[i];
            if (i >= NP_WINDOW)
                window -= p[i - NP_WINDOW];
            if (i >= NP_WINDOW - 1) {
                double avg = window / NP_WINDOW;
                sum4 += avg * avg * avg * avg;
                values++;
            }
        }
        return round(Math.pow(sum4 / values, 0.25));
    }

    /** Highest average over {@code window} consecutive seconds; 0 if the series is shorter. */
    static int bestAverage(double[] p, int window) {
        if (p.length < window)
            return 0;
        double sum = 0, best = 0;
        for (int i = 0; i < p.length; i++) {
            sum += p[i];
            if (i >= window)
                sum -= p[i - window];
            if (i >= window - 1)
                best = Math.max(best, sum);
        }
        return round(best / window);
    }

    private static double mean(double[] v) {
        double sum = 0;
        for (double x : v)
            sum += x;
        return sum / v.length;
    }

    private static int round(double v) {
        return (int) Math.round(v);
    }

    /**
     * Sets a ride's metrics if they are missing or outdated, but only when the samples cover at
     * least 95% of the summarized duration: older builds deleted samples, and metrics from a
     * fragment of a ride would be wrong. The ride is marked as done either way, so it isn't
     * retried on every "Compact data".
     *
     * @return whether the exercise changed
     */
    public static boolean fillIn(Exercise exercise, List<ExerciseData> samples) {
        if (exercise.getMetricsVersion() >= VERSION)
            return false;
        RideMetrics metrics = of(samples);
        if (metrics != null && metrics.seconds >= 0.95 * exercise.getTotalDuration())
            metrics.applyTo(exercise);
        else
            exercise.setMetricsVersion(VERSION);
        return true;
    }

    /**
     * Stores the metrics that are new since DB v9 (NP, best efforts). The averages are not
     * touched: {@link WorkoutSummary} sets them once, when the ride is summarized.
     */
    public void applyTo(Exercise exercise) {
        exercise.setNormPower(normPower);
        exercise.setBest5min(best5min);
        exercise.setBest20min(best20min);
        exercise.setBest60min(best60min);
        exercise.setMetricsVersion(VERSION);
    }
}
