package eu.le_tian.iConsoleOS.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eu.le_tian.iConsoleOS.R;
import eu.le_tian.iConsoleOS.data.Exercise;

public class StoredWorkoutAdapter extends RecyclerView.Adapter<StoredWorkoutAdapter.WorkoutViewHolder> {

    private List<Exercise> exerciseList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @SuppressLint("NotifyDataSetChanged")
    public void setExercises(List<Exercise> exercises) {
        this.exerciseList = exercises != null ? exercises : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stored_workout, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        Exercise exercise = exerciseList.get(position);
        holder.bind(exercise, dateFormat);
    }

    @Override
    public int getItemCount() {
        return exerciseList.size();
    }

    static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvWorkoutTitle;
        private final TextView tvWorkoutStatus;
        private final TextView tvWorkoutDate;
        private final TextView tvWorkoutDetails;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWorkoutTitle = itemView.findViewById(R.id.tvWorkoutTitle);
            tvWorkoutStatus = itemView.findViewById(R.id.tvWorkoutStatus);
            tvWorkoutDate = itemView.findViewById(R.id.tvWorkoutDate);
            tvWorkoutDetails = itemView.findViewById(R.id.tvWorkoutDetails);
        }

        @SuppressLint("SetTextI18n")
        public void bind(Exercise exercise, SimpleDateFormat dateFormat) {
            String profileName = exercise.getExProfileName() != null ? exercise.getExProfileName() : "Workout";
            tvWorkoutTitle.setText("#" + exercise.getExerciseID() + " — " + profileName);

            String dateStr = exercise.getExStartDateTime() > 0 ? dateFormat.format(new Date(exercise.getExStartDateTime())) : "N/A";
            tvWorkoutDate.setText("Started: " + dateStr);

            if (exercise.isSummarized()) {
                tvWorkoutStatus.setText("SUMMARIZED");
                tvWorkoutStatus.setBackgroundResource(R.drawable.bg_status_summarized);

                long durSeconds = exercise.getTotalDuration();
                long mins = durSeconds / 60;
                long secs = durSeconds % 60;
                String durationStr = String.format(Locale.US, "%d:%02d", mins, secs);
                double distKm = exercise.getTotalDistance() / 10.0;
                double avgSpeedKm = exercise.getAvgSpeed() / 10.0;

                String details = String.format(Locale.US,
                        "Duration: %s  |  Distance: %.1f km  |  Calories: %d kcal\n" +
                        "Avg Power: %d W  |  Avg Speed: %.1f km/h  |  Avg HR: %d bpm  |  Avg RPM: %d",
                        durationStr, distKm, exercise.getTotalCalories(),
                        exercise.getAvgPower(), avgSpeedKm, exercise.getAvgHR(), exercise.getAvgRPM());
                tvWorkoutDetails.setText(details);
            } else {
                tvWorkoutStatus.setText("RAW DATA");
                tvWorkoutStatus.setBackgroundResource(R.drawable.bg_status_raw);
                tvWorkoutDetails.setText("Raw sensor data points stored (not summarized yet).");
            }
        }
    }
}
