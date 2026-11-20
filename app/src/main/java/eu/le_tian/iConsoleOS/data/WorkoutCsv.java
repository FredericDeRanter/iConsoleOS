package eu.le_tian.iConsoleOS.data;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * CSV export of workouts: one row per sample, or one summary row for a workout whose samples were
 * deleted by older builds. Same columns and date format as the old export, so existing
 * spreadsheets keep working; fields are quoted only when needed (RFC 4180). Plain Java.
 */
public final class WorkoutCsv {
    static final List<String> HEADER = Arrays.asList("ExerciseID", "ProfileName", "StartDateTime", "UserID",
            "Calories", "Distance", "HeartRate", "Power", "RPM", "Speed", "TimeStamp");

    /** Where the samples of a workout come from (the repository decodes compacted ones). */
    public interface SampleSource {
        List<ExerciseData> samples(long exerciseID) throws Exception;
    }

    /** What was written. */
    public static final class Result {
        public final int workouts;
        public final int rows;

        Result(int workouts, int rows) {
            this.workouts = workouts;
            this.rows = rows;
        }
    }

    private WorkoutCsv() {
    }

    public static Result write(Writer out, List<Exercise> exercises, SampleSource source, TimeZone zone) throws Exception {
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z", Locale.US);
        date.setTimeZone(zone);
        writeRow(out, HEADER);
        int workouts = 0, rows = 0;
        for (Exercise ex : exercises) {
            String id = Long.toString(ex.getExerciseID());
            String start = date.format(new Date(ex.getExStartDateTime()));
            String user = Long.toString(ex.getUserParentID());
            List<ExerciseData> samples = source.samples(ex.getExerciseID());
            if (samples != null && !samples.isEmpty()) {
                for (ExerciseData d : samples) {
                    writeRow(out, Arrays.asList(id, ex.getExProfileName(), start, user,
                            Integer.toString(d.getExCalories()), Integer.toString(d.getExDistance()),
                            Integer.toString(d.getExHR()), Integer.toString(d.getExPower()),
                            Integer.toString(d.getExRPM()), Integer.toString(d.getExSpeed()),
                            Long.toString(d.getExTimestamp())));
                    rows++;
                }
                workouts++;
            } else if (ex.isSummarized()) {
                // Samples gone: one row with the totals/averages (as the old export did).
                writeRow(out, Arrays.asList(id, ex.getExProfileName(), start, user,
                        Integer.toString(ex.getTotalCalories()), Integer.toString(ex.getTotalDistance()),
                        Integer.toString(ex.getAvgHR()), Integer.toString(ex.getAvgPower()),
                        Integer.toString(ex.getAvgRPM()), Integer.toString(ex.getAvgSpeed()),
                        Long.toString(ex.getTotalDuration())));
                rows++;
                workouts++;
            }
        }
        out.flush();
        return new Result(workouts, rows);
    }

    static void writeRow(Writer out, List<String> fields) throws IOException {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0)
                out.write(',');
            out.write(field(fields.get(i)));
        }
        out.write('\n');
    }

    static String field(String value) {
        if (value == null)
            return "";
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0)
            return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
