package eu.le_tian.iConsoleOS.data;

import org.junit.Test;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class WorkoutCsvTest {
    private static final TimeZone BRUSSELS = TimeZone.getTimeZone("Europe/Brussels");

    @Test
    public void samplesAndSummaryRows() throws Exception {
        Exercise withSamples = new Exercise(1089, 1795199055000L, 0, "Quick Start", 1);
        Exercise summarizedOnly = new Exercise(1088, 1795039730000L, 0, "Interval training", 1);
        summarizedOnly.setSummarized(true);
        summarizedOnly.setTotalDuration(2826);
        summarizedOnly.setAvgPower(1195);
        summarizedOnly.setAvgHR(8);
        Exercise empty = new Exercise(1090, 1795300000000L, 0, "Quick Start", 1); // never recorded anything

        StringWriter out = new StringWriter();
        WorkoutCsv.Result r = WorkoutCsv.write(out, Arrays.asList(withSamples, summarizedOnly, empty),
                id -> id == 1089 ? Arrays.asList(
                        new ExerciseData(0, 1000, 250, 1, 0, 120, 70, 0, 1089),
                        new ExerciseData(0, 1010, 251, 2, 1, 121, 71, 1, 1089)) : Collections.<ExerciseData>emptyList(),
                BRUSSELS);

        String[] lines = out.toString().split("\n");
        assertEquals("ExerciseID,ProfileName,StartDateTime,UserID,Calories,Distance,HeartRate,Power,RPM,Speed,TimeStamp", lines[0]);
        assertEquals("1089,Quick Start,2026-11-20T19:24:15 +0100,1,0,1,120,1000,70,250,0", lines[1]);
        assertEquals("1089,Quick Start,2026-11-20T19:24:15 +0100,1,1,2,121,1010,71,251,1", lines[2]);
        assertEquals("1088,Interval training,2026-11-18T23:08:50 +0100,1,0,0,8,1195,0,0,2826", lines[3]);
        assertEquals(4, lines.length);
        assertEquals(2, r.workouts);
        assertEquals(3, r.rows);
    }

    @Test
    public void fieldsAreQuotedOnlyWhenNeeded() {
        assertEquals("Quick Start", WorkoutCsv.field("Quick Start"));
        assertEquals("\"Intervals, hard\"", WorkoutCsv.field("Intervals, hard"));
        assertEquals("\"The \"\"big\"\" one\"", WorkoutCsv.field("The \"big\" one"));
        assertEquals("\"two\nlines\"", WorkoutCsv.field("two\nlines"));
        assertEquals("", WorkoutCsv.field(null));
    }

    @Test
    public void commaInProfileNameKeepsColumns() throws Exception {
        StringWriter out = new StringWriter();
        List<Exercise> one = Collections.singletonList(new Exercise(1, 0, 0, "Hills, short", 1));
        WorkoutCsv.write(out, one, id -> Collections.singletonList(new ExerciseData(0, 1, 2, 3, 4, 5, 6, 7, 1)),
                TimeZone.getTimeZone("UTC"));
        assertEquals("1,\"Hills, short\",1970-01-01T00:00:00 +0000,1,4,3,5,1,6,2,7", out.toString().split("\n")[1]);
    }
}
