package eu.le_tian.iConsoleOS.data;

import java.util.List;

/**
 * Totals, averages and maxima of one workout's samples. Plain Java so it can be unit tested.
 * Units follow {@link ExerciseData}: power, speed and distance are stored ×10.
 *
 * <p>Averages are time-weighted (every second of the ride counts once, see {@link RideMetrics});
 * rides summarized before DB v9 have plain per-sample averages, which are kept as they are.
 */
public final class WorkoutSummary {
    public final long totalDuration; // seconds: timestamp of the last sample
    public final int totalDistance;
    public final int totalCalories;
    public final int avgPower, maxPower;
    public final int avgSpeed, maxSpeed;
    public final int avgHR, maxHR;
    public final int avgRPM, maxRPM;

    private WorkoutSummary(long totalDuration, int totalDistance, int totalCalories,
                           int avgPower, int maxPower, int avgSpeed, int maxSpeed,
                           int avgHR, int maxHR, int avgRPM, int maxRPM) {
        this.totalDuration = totalDuration;
        this.totalDistance = totalDistance;
        this.totalCalories = totalCalories;
        this.avgPower = avgPower;
        this.maxPower = maxPower;
        this.avgSpeed = avgSpeed;
        this.maxSpeed = maxSpeed;
        this.avgHR = avgHR;
        this.maxHR = maxHR;
        this.avgRPM = avgRPM;
        this.maxRPM = maxRPM;
    }

    /** @return the summary, or null when there are no samples */
    public static WorkoutSummary of(List<ExerciseData> samples) {
        if (samples == null || samples.isEmpty())
            return null;

        int maxPower = 0, maxSpeed = 0, maxHR = 0, maxRPM = 0, maxDistance = 0, maxCalories = 0;
        for (ExerciseData d : samples) {
            maxPower = Math.max(maxPower, d.getExPower());
            maxSpeed = Math.max(maxSpeed, d.getExSpeed());
            maxHR = Math.max(maxHR, d.getExHR());
            maxRPM = Math.max(maxRPM, d.getExRPM());
            maxDistance = Math.max(maxDistance, d.getExDistance());
            maxCalories = Math.max(maxCalories, d.getExCalories());
        }
        RideMetrics m = RideMetrics.of(samples);
        return new WorkoutSummary(samples.get(samples.size() - 1).getExTimestamp(), maxDistance, maxCalories,
                m.avgPower, maxPower, m.avgSpeed, maxSpeed,
                m.avgHR, maxHR, m.avgRPM, maxRPM);
    }

    public void applyTo(Exercise exercise) {
        exercise.setTotalDuration(totalDuration);
        exercise.setTotalDistance(totalDistance);
        exercise.setTotalCalories(totalCalories);
        exercise.setAvgPower(avgPower);
        exercise.setMaxPower(maxPower);
        exercise.setAvgSpeed(avgSpeed);
        exercise.setMaxSpeed(maxSpeed);
        exercise.setAvgHR(avgHR);
        exercise.setMaxHR(maxHR);
        exercise.setAvgRPM(avgRPM);
        exercise.setMaxRPM(maxRPM);
    }
}
