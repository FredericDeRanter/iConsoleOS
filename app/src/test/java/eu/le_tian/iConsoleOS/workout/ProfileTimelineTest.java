package eu.le_tian.iConsoleOS.workout;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import eu.le_tian.iConsoleOS.data.ExerciseProfileData;
import eu.le_tian.iConsoleOS.workout.ProfileTimeline.Step;

import static eu.le_tian.iConsoleOS.workout.ProfileTimeline.COOL_DOWN;
import static eu.le_tian.iConsoleOS.workout.ProfileTimeline.INTERVAL;
import static eu.le_tian.iConsoleOS.workout.ProfileTimeline.WARM_UP;
import static org.junit.Assert.assertEquals;

public class ProfileTimelineTest {

    private static ExerciseProfileData step(int type, int level, int seconds) {
        return new ExerciseProfileData(0, 0, type, level, seconds, 1);
    }

    /** "Interval training" from the default profiles: 2 min L5, (5 min L8, 40 s L28), 2 min L4. */
    private static final List<ExerciseProfileData> INTERVALS = Arrays.asList(
            step(WARM_UP, 5, 120), step(INTERVAL, 8, 300), step(INTERVAL, 28, 40), step(COOL_DOWN, 4, 120));

    private static List<Step> steps(int... secondLevel) {
        List<Step> l = new ArrayList<>();
        for (int i = 0; i < secondLevel.length; i += 2)
            l.add(new Step(secondLevel[i], secondLevel[i + 1]));
        return l;
    }

    @Test
    public void quickStart() {
        assertEquals(steps(0, 3), ProfileTimeline.build(null, 0, true, 3));
        assertEquals(steps(0, 3), ProfileTimeline.build(Collections.emptyList(), 0, false, 3));
        assertEquals("with a duration", steps(0, 3, 600, 3), ProfileTimeline.build(null, 600, true, 3));
    }

    @Test
    public void minDurationIsTheSumOfSteps() {
        assertEquals(580, ProfileTimeline.minDuration(INTERVALS));
        assertEquals(0, ProfileTimeline.minDuration(null));
    }

    @Test
    public void atItsOwnLengthTheProfileRunsAsIs() {
        List<Step> expected = steps(0, 5, 120, 8, 420, 28, 460, 4);
        assertEquals(expected, ProfileTimeline.build(INTERVALS, 580, true, 1));
        assertEquals(expected, ProfileTimeline.build(INTERVALS, 580, false, 1));
    }

    @Test
    public void repeatFillsWithIntervalsAndCutsTheLastOne() {
        // 2500 s: cool-down from 2380; six full (300 + 40) rounds, then 220 s of the 5-minute step.
        assertEquals(steps(0, 5,
                        120, 8, 420, 28, 460, 8, 760, 28, 800, 8, 1100, 28, 1140, 8, 1440, 28,
                        1480, 8, 1780, 28, 1820, 8, 2120, 28, 2160, 8,
                        2380, 4),
                ProfileTimeline.build(INTERVALS, 2500, true, 1));
    }

    @Test
    public void stretchMakesEveryIntervalLonger() {
        // Intervals get 2260 s instead of 340: 300 s -> 1994 s, the last one takes the rest (266 s).
        assertEquals(steps(0, 5, 120, 8, 2114, 28, 2380, 4),
                ProfileTimeline.build(INTERVALS, 2500, false, 1));
    }

    @Test(timeout = 2000)
    public void zeroSecondIntervalsDontHang() {
        List<ExerciseProfileData> p = Arrays.asList(step(WARM_UP, 5, 60), step(INTERVAL, 8, 0), step(COOL_DOWN, 4, 60));
        assertEquals(steps(0, 5, 60, 8, 540, 4), ProfileTimeline.build(p, 600, true, 1));
        List<ExerciseProfileData> two = Arrays.asList(step(INTERVAL, 8, 0), step(INTERVAL, 9, 0));
        assertEquals(steps(0, 8, 0, 9), ProfileTimeline.build(two, 600, false, 1));
    }

    @Test
    public void unknownStepTypesAreIgnored() {
        List<ExerciseProfileData> p = Arrays.asList(step(WARM_UP, 5, 60), step(7, 99, 60), step(COOL_DOWN, 4, 60));
        assertEquals(steps(0, 5, 60, 4), ProfileTimeline.build(p, 120, true, 1));
    }

    /** Same results as the code it replaced (IConsoleViewModel.CreateTimeStampsAndLevels). */
    @Test
    public void matchesTheOldImplementation() {
        Random r = new Random(42);
        for (int n = 0; n < 5000; n++) {
            List<ExerciseProfileData> p = new ArrayList<>();
            int warm = r.nextInt(3), intervals = r.nextInt(4), cool = r.nextInt(3);
            for (int i = 0; i < warm; i++) p.add(step(WARM_UP, 1 + r.nextInt(30), 1 + r.nextInt(600)));
            for (int i = 0; i < intervals; i++) p.add(step(INTERVAL, 1 + r.nextInt(30), 1 + r.nextInt(600)));
            for (int i = 0; i < cool; i++) p.add(step(COOL_DOWN, 1 + r.nextInt(30), 1 + r.nextInt(600)));
            if (p.isEmpty())
                continue;
            int duration = ProfileTimeline.minDuration(p) + r.nextInt(3600);
            boolean repeat = r.nextBoolean();
            assertEquals(p + " " + duration + " " + repeat,
                    legacy(p, duration, repeat), ProfileTimeline.build(p, duration, repeat, 1));
        }
    }

    /** The old algorithm, copied verbatim (Point -> Step, logging removed). */
    private static List<Step> legacy(List<ExerciseProfileData> exerciseProfileDataList, int duration, boolean repeat) {
        List<Step> tempTimeStampsAndLevels = new ArrayList<>();
        int totalWarmUp = 0, totalCoolDown = 0, totalExercise = 0, indexOfFirstExercise = 0, indexOfFirstCoolDown = 0;
        for (int i = 0; i < exerciseProfileDataList.size(); i++) {
            int dataType = exerciseProfileDataList.get(i).getDataType();
            if (dataType == 0) {
                totalWarmUp += exerciseProfileDataList.get(i).getDuration();
                indexOfFirstExercise = i + 1;
            } else if (dataType == 1) {
                totalExercise += exerciseProfileDataList.get(i).getDuration();
                indexOfFirstCoolDown = i + 1;
            } else if (dataType == 2) {
                totalCoolDown += exerciseProfileDataList.get(i).getDuration();
            }
        }
        int exerciseDuration = duration - totalCoolDown;
        int movingDuration = 0;
        if (repeat) {
            for (int i = 0; i < exerciseProfileDataList.size(); i++) {
                int dataType = exerciseProfileDataList.get(i).getDataType();
                if (dataType == 0) {
                    tempTimeStampsAndLevels.add(new Step(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                    movingDuration += exerciseProfileDataList.get(i).getDuration();
                } else if (dataType == 1) {
                    int tempDuration = exerciseProfileDataList.get(i).getDuration();
                    int durationLeftForExercise = exerciseDuration - movingDuration;
                    if (durationLeftForExercise > tempDuration) {
                        tempTimeStampsAndLevels.add(new Step(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                        movingDuration += tempDuration;
                        if (i + 1 == indexOfFirstCoolDown) {
                            i = indexOfFirstExercise - 1;
                        }
                    } else {
                        tempTimeStampsAndLevels.add(new Step(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                        movingDuration += durationLeftForExercise;
                        i = indexOfFirstCoolDown - 1;
                    }
                } else if (dataType == 2) {
                    tempTimeStampsAndLevels.add(new Step(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                    movingDuration += exerciseProfileDataList.get(i).getDuration();
                }
            }
        } else {
            for (int i = 0; i < exerciseProfileDataList.size(); i++) {
                int dataType = exerciseProfileDataList.get(i).getDataType();
                if (dataType == 0) {
                    tempTimeStampsAndLevels.add(new Step(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                    movingDuration += exerciseProfileDataList.get(i).getDuration();
                } else if (dataType == 1) {
                    int tempDuration = exerciseProfileDataList.get(i).getDuration();
                    int durationLeftForExercise = exerciseDuration - movingDuration;
                    int actualDuration;
                    if (i + 1 == indexOfFirstCoolDown) {
                        actualDuration = durationLeftForExercise;
                    } else {
                        int totalDurationForExercise = exerciseDuration - totalWarmUp;
                        double factor = (double) totalDurationForExercise / totalExercise;
                        actualDuration = (int) (tempDuration * factor);
                    }
                    tempTimeStampsAndLevels.add(new Step(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                    movingDuration += actualDuration;
                } else if (dataType == 2) {
                    tempTimeStampsAndLevels.add(new Step(movingDuration, exerciseProfileDataList.get(i).getRelativeLevel()));
                    movingDuration += exerciseProfileDataList.get(i).getDuration();
                }
            }
        }
        return tempTimeStampsAndLevels;
    }
}
