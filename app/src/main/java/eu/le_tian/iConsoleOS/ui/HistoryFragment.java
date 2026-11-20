package eu.le_tian.iConsoleOS.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Locale;

import eu.le_tian.iConsoleOS.databinding.FragmentHistoryBinding;

/**
 * This fragment is where you can see the done exercises and database tools.
 */

public class HistoryFragment extends Fragment {

    private IConsoleViewModel IConsoleViewModel;
    private FragmentHistoryBinding binding;
    private Resources res;
    private StoredWorkoutAdapter workoutAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        IConsoleViewModel = new ViewModelProvider(this).get(IConsoleViewModel.class);
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        res = getResources();

        workoutAdapter = new StoredWorkoutAdapter();
        binding.rvStoredWorkouts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvStoredWorkouts.setAdapter(workoutAdapter);

        binding.exportExerciseBtn.setOnClickListener(v -> writeExercisesToCSV());

        binding.btnSummarizeWorkouts.setOnClickListener(v -> {
            if (IConsoleViewModel == null) return;
            binding.btnSummarizeWorkouts.setEnabled(false);
            Toast.makeText(getContext(), "Summarizing workouts and cleaning up data...", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                try {
                    int count = IConsoleViewModel.summarizeAllUnsummarizedExercises().get();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null) {
                                binding.btnSummarizeWorkouts.setEnabled(true);
                                String msg = count > 0 ? "Successfully summarized " + count + " workout(s)." : "All workouts are already summarized.";
                                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                                updateDatabaseStats();
                            }
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null) {
                                binding.btnSummarizeWorkouts.setEnabled(true);
                                Toast.makeText(getContext(), "Error during cleanup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }).start();
        });

        updateDatabaseStats();
    }

    private void updateDatabaseStats() {
        if (binding == null || IConsoleViewModel == null) return;

        // Display current DB size
        String dbSize = IConsoleViewModel.getFormattedDatabaseSize();
        binding.tvDatabaseSize.setText("Database Size: " + dbSize);

        // Observe total workouts count
        IConsoleViewModel.getExerciseCount().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) {
                int total = count != null ? count : 0;
                binding.tvExerciseCount.setText("Workouts Recorded: " + total);
            }
        });

        // Observe summarized workouts count
        IConsoleViewModel.getSummarizedExerciseCount().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) {
                int total = count != null ? count : 0;
                binding.tvSummarizedCount.setText("Summarized Workouts (Cleaned): " + total);
            }
        });

        // Observe unsummarized workouts count
        IConsoleViewModel.getUnsummarizedExerciseCount().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) {
                int total = count != null ? count : 0;
                binding.tvUnsummarizedCount.setText("Detailed Workouts (Unsummarized): " + total);
            }
        });

        // Observe total sample points count
        IConsoleViewModel.getExerciseDataCount().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) {
                int total = count != null ? count : 0;
                binding.tvDataPointCount.setText("Raw Sample Points Stored: " + String.format(Locale.US, "%,d", total));
            }
        });

        // Observe stored exercises list
        IConsoleViewModel.getAllExercisesLive().observe(getViewLifecycleOwner(), exercises -> {
            if (binding != null && workoutAdapter != null && exercises != null) {
                workoutAdapter.setExercises(exercises);
            }
        });
    }

    private void writeExercisesToCSV() {
        IConsoleViewModel.writeExercisesToCSV((long) 1);
    }
}
