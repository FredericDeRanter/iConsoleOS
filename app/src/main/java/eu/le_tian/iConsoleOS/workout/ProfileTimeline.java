package eu.le_tian.iConsoleOS.workout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.le_tian.iConsoleOS.data.ExerciseProfileData;

/**
 * Turns a training profile into a level timeline: the second at which each level starts. Plain
 * Java (no Android {@code Point}) so it can be unit tested.
 *
 * <p>A profile has warm-up steps (data type 0), interval steps (1) and cool-down steps (2), in
 * that order. For a chosen duration longer than the profile:
 * <ul>
 *   <li><b>repeat</b>: the interval steps are repeated until the cool-down has to start; the last
 *   interval step is cut short to fit;</li>
 *   <li><b>stretch</b> (no repeat): every interval step is made longer by the same factor, the
 *   last one takes what is left.</li>
 * </ul>
 * Warm-up and cool-down keep their length. Without steps (Quick Start) the timeline is the
 * default level from 0 (and up to the duration, if there is one).
 */
public final class ProfileTimeline {
    public static final int WARM_UP = 0, INTERVAL = 1, COOL_DOWN = 2;

    private ProfileTimeline() {
    }

    /** One level change: from {@code second} on, the level is {@code level}. */
    public static final class Step {
        public final int second;
        public final int level;

        public Step(int second, int level) {
            this.second = second;
            this.level = level;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Step && ((Step) o).second == second && ((Step) o).level == level;
        }

        @Override
        public int hashCode() {
            return 31 * second + level;
        }

        @Override
        public String toString() {
            return "(" + second + "s, L" + level + ")";
        }
    }

    /** The profile's own length: the sum of its steps (the shortest duration it can run). */
    public static int minDuration(List<ExerciseProfileData> steps) {
        int total = 0;
        if (steps != null) {
            for (ExerciseProfileData s : steps)
                total += s.getDuration();
        }
        return total;
    }

    /**
     * The level timeline, never empty.
     *
     * @param duration     total seconds (0 = open-ended Quick Start); not shorter than
     *                     {@link #minDuration} when there are steps
     * @param defaultLevel the level when the profile has no steps
     */
    public static List<Step> build(List<ExerciseProfileData> steps, int duration, boolean repeat, int defaultLevel) {
        List<Step> out = new ArrayList<>();
        if (steps == null || steps.isEmpty()) {
            out.add(new Step(0, defaultLevel));
            if (duration > 0)
                out.add(new Step(duration, defaultLevel)); // so the graph shows the whole ride
            return out;
        }

        int totalWarmUp = 0, totalCoolDown = 0, totalIntervals = 0;
        int indexOfFirstInterval = 0, indexOfFirstCoolDown = 0;
        for (int i = 0; i < steps.size(); i++) {
            int type = steps.get(i).getDataType();
            if (type == WARM_UP) {
                totalWarmUp += steps.get(i).getDuration();
                indexOfFirstInterval = i + 1;
            } else if (type == INTERVAL) {
                totalIntervals += steps.get(i).getDuration();
                indexOfFirstCoolDown = i + 1;
            } else if (type == COOL_DOWN) {
                totalCoolDown += steps.get(i).getDuration();
            }
        }
        int intervalsEnd = duration - totalCoolDown; // the second the cool-down starts
        int t = 0;

        for (int i = 0; i < steps.size(); i++) {
            ExerciseProfileData step = steps.get(i);
            int type = step.getDataType();
            if (type == INTERVAL) {
                int left = intervalsEnd - t;
                out.add(new Step(t, step.getRelativeLevel()));
                if (repeat) {
                    // totalIntervals > 0: with only 0-second intervals this would never get anywhere
                    if (left > step.getDuration() && totalIntervals > 0) {
                        t += step.getDuration();
                        if (i + 1 == indexOfFirstCoolDown)
                            i = indexOfFirstInterval - 1; // back to the first interval
                    } else {
                        t += left; // last repeat: cut short, then cool down
                        i = indexOfFirstCoolDown - 1;
                    }
                } else if (i + 1 == indexOfFirstCoolDown) {
                    t += left; // last interval takes what is left
                } else {
                    double factor = totalIntervals > 0 ? (double) (intervalsEnd - totalWarmUp) / totalIntervals : 0;
                    t += (int) (step.getDuration() * factor);
                }
            } else if (type == WARM_UP || type == COOL_DOWN) {
                out.add(new Step(t, step.getRelativeLevel()));
                t += step.getDuration();
            } // other types: ignored, as before
        }
        return Collections.unmodifiableList(out);
    }
}
