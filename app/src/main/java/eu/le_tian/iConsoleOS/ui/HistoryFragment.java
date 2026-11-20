package eu.le_tian.iConsoleOS.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import eu.le_tian.iConsoleOS.databinding.FragmentHistoryBinding;

/**
 * This fragment is where you can see the done exercises and database tools.
 */

public class HistoryFragment extends Fragment {

    private IConsoleViewModel IConsoleViewModel;
    private FragmentHistoryBinding binding;
    private Resources res;
    private StoredWorkoutAdapter workoutAdapter;
    private long rawSampleCount = 0;
    private long compactedSampleCount = 0;
    private static final String TAG = "HistoryFragment";

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
            new AlertDialog.Builder(requireContext())
                    .setTitle("Compact data")
                    .setMessage("Packs the per-second samples of finished workouts into a compact form "
                            + "(nothing is thrown away) and then shrinks the database file.\n\n"
                            + "This rewrites the whole database. Make a backup first if you don't have a recent one.")
                    .setPositiveButton("Compact", (d, w) -> compactData())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        updateDatabaseStats();
    }

    private void compactData() {
        binding.btnSummarizeWorkouts.setEnabled(false);
        Toast.makeText(getContext(), "Compacting data...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            String msg;
            try {
                int count = IConsoleViewModel.compactAllExercises().get();
                msg = count > 0 ? "Compacted " + count + " workout(s), database shrunk."
                        : "Nothing to compact, database shrunk.";
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                Log.e(TAG, "Compacting failed", cause);
                msg = "Compacting failed: " + cause.getMessage();
            } catch (InterruptedException e) {
                msg = "Compacting interrupted";
            }
            final String result = msg;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.btnSummarizeWorkouts.setEnabled(true);
                    refreshDatabaseSize();
                    new AlertDialog.Builder(requireContext())
                            .setMessage(result)
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        }).start();
    }

    private void refreshDatabaseSize() {
        if (binding == null || IConsoleViewModel == null) return;
        binding.tvDatabaseSize.setText("Database Size: " + IConsoleViewModel.getFormattedDatabaseSize());
    }

    private void updateDatabaseStats() {
        if (binding == null || IConsoleViewModel == null) return;

        refreshDatabaseSize();

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
                binding.tvSummarizedCount.setText("Summarized Workouts: " + total);
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
                rawSampleCount = total;
                showSampleCounts();
            }
        });

        IConsoleViewModel.getCompactedSampleCount().observe(getViewLifecycleOwner(), count -> {
            compactedSampleCount = count != null ? count : 0;
            showSampleCounts();
        });

        // Observe stored exercises list
        IConsoleViewModel.getAllExercisesLive().observe(getViewLifecycleOwner(), exercises -> {
            if (binding != null && workoutAdapter != null && exercises != null) {
                workoutAdapter.setExercises(exercises);
            }
        });
    }

    private void showSampleCounts() {
        if (binding == null) return;
        binding.tvDataPointCount.setText(String.format(Locale.US,
                "Samples: %,d compacted, %,d not compacted yet", compactedSampleCount, rawSampleCount));
    }

    private void writeExercisesToCSV() {
        IConsoleViewModel.writeExercisesToCSV((long) 1);
    }
}
