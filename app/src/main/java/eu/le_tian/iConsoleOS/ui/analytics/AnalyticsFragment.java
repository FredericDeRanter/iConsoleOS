package eu.le_tian.iConsoleOS.ui.analytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.le_tian.iConsoleOS.data.Exercise;
import eu.le_tian.iConsoleOS.databinding.FragmentAnalyticsBinding;
import eu.le_tian.iConsoleOS.ui.IConsoleViewModel;

public class AnalyticsFragment extends Fragment {

    private FragmentAnalyticsBinding binding;
    private IConsoleViewModel viewModel;
    private List<Exercise> allExercises = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(IConsoleViewModel.class);
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupSpinners();

        viewModel.getAllExercisesChronologicalLive().observe(getViewLifecycleOwner(), exercises -> {
            allExercises = exercises != null ? exercises : new ArrayList<>();
            updateAnalyticsData();
        });
    }

    private void setupSpinners() {
        String[] timeframes = {"Last 1 Month", "Last 3 Months", "Last 6 Months", "Last 12 Months", "All Time"};
        ArrayAdapter<String> tfAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, timeframes);
        tfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTimeframe.setAdapter(tfAdapter);
        binding.spinnerTimeframe.setSelection(1); // Default to Last 3 Months

        String[] groupings = {"Per Week", "Per Month"};
        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, groupings);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGrouping.setAdapter(groupAdapter);

        String[] metrics = {"Distance (km)", "Duration (min)", "Calories (kcal)", "Workouts Count"};
        ArrayAdapter<String> metricAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, metrics);
        metricAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMetric.setAdapter(metricAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                updateAnalyticsData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        binding.spinnerTimeframe.setOnItemSelectedListener(listener);
        binding.spinnerGrouping.setOnItemSelectedListener(listener);
        binding.spinnerMetric.setOnItemSelectedListener(listener);
    }

    private void updateAnalyticsData() {
        if (binding == null) return;

        int timeframePos = binding.spinnerTimeframe.getSelectedItemPosition();
        int groupingPos = binding.spinnerGrouping.getSelectedItemPosition();
        int metricPos = binding.spinnerMetric.getSelectedItemPosition();

        long cutoffTime = getCutoffTimestamp(timeframePos);

        // Filter exercises
        List<Exercise> filtered = new ArrayList<>();
        for (Exercise ex : allExercises) {
            if (ex.getExStartDateTime() >= cutoffTime) {
                filtered.add(ex);
            }
        }

        // Group exercises into buckets
        Map<String, Bucket> bucketMap = groupExercises(filtered, groupingPos, timeframePos);

        // Compute summary metrics for timeframe
        double totalDist = 0;
        double totalDurMin = 0;
        double totalCal = 0;
        int totalWorkouts = filtered.size();

        for (Exercise ex : filtered) {
            totalDist += ex.getTotalDistance() / 10.0;
            totalDurMin += ex.getTotalDuration() / 60.0;
            totalCal += ex.getTotalCalories();
        }

        binding.tvSummaryDistance.setText(String.format(Locale.US, "%.1f km", totalDist));
        binding.tvSummaryDuration.setText(String.format(Locale.US, "%.0f min", totalDurMin));
        binding.tvSummaryCalories.setText(String.format(Locale.US, "%.0f kcal", totalCal));
        binding.tvSummaryWorkouts.setText(String.valueOf(totalWorkouts));

        // Create graph data points
        List<AnalyticsGraphView.DataPoint> points = new ArrayList<>();
        String unitLabel = getUnitLabel(metricPos);

        for (Map.Entry<String, Bucket> entry : bucketMap.entrySet()) {
            Bucket b = entry.getValue();
            double val = 0;
            switch (metricPos) {
                case 0: val = b.distance; break;
                case 1: val = b.durationMin; break;
                case 2: val = b.calories; break;
                case 3: val = b.count; break;
            }
            points.add(new AnalyticsGraphView.DataPoint(b.displayLabel, val, b.count, b.distance, b.durationMin, b.calories));
        }

        binding.analyticsGraphView.setDataPoints(points, unitLabel);
    }

    private long getCutoffTimestamp(int timeframePos) {
        Calendar cal = Calendar.getInstance();
        switch (timeframePos) {
            case 0: cal.add(Calendar.MONTH, -1); return cal.getTimeInMillis();
            case 1: cal.add(Calendar.MONTH, -3); return cal.getTimeInMillis();
            case 2: cal.add(Calendar.MONTH, -6); return cal.getTimeInMillis();
            case 3: cal.add(Calendar.YEAR, -1); return cal.getTimeInMillis();
            case 4: default: return 0; // All time
        }
    }

    private String getUnitLabel(int metricPos) {
        switch (metricPos) {
            case 0: return "km";
            case 1: return "min";
            case 2: return "kcal";
            case 3: return "workouts";
            default: return "";
        }
    }

    private Map<String, Bucket> groupExercises(List<Exercise> exercises, int groupingPos, int timeframePos) {
        Map<String, Bucket> map = new LinkedHashMap<>();

        boolean perMonth = (groupingPos == 1);
        SimpleDateFormat keyFmt = new SimpleDateFormat(perMonth ? "yyyy-MM" : "yyyy-'W'ww", Locale.getDefault());
        SimpleDateFormat labelFmt = new SimpleDateFormat(perMonth ? "MMM yy" : "'W'ww", Locale.getDefault());

        // Fill empty buckets across timeframe range for smooth timeline
        long now = System.currentTimeMillis();
        long start = getCutoffTimestamp(timeframePos);
        if (start == 0 && !exercises.isEmpty()) {
            start = exercises.get(0).getExStartDateTime();
        } else if (start == 0) {
            start = now - (90L * 24 * 60 * 60 * 1000); // Default 3 months back
        }

        Calendar stepCal = Calendar.getInstance();
        stepCal.setTimeInMillis(start);

        while (stepCal.getTimeInMillis() <= now + (7L * 24 * 60 * 60 * 1000)) {
            Date d = stepCal.getTime();
            String key = keyFmt.format(d);
            if (!map.containsKey(key)) {
                map.put(key, new Bucket(labelFmt.format(d)));
            }
            if (perMonth) {
                stepCal.add(Calendar.MONTH, 1);
            } else {
                stepCal.add(Calendar.WEEK_OF_YEAR, 1);
            }
        }

        // Aggregate actual exercises into buckets
        for (Exercise ex : exercises) {
            if (ex.getExStartDateTime() <= 0) continue;
            Date d = new Date(ex.getExStartDateTime());
            String key = keyFmt.format(d);
            Bucket b = map.get(key);
            if (b == null) {
                b = new Bucket(labelFmt.format(d));
                map.put(key, b);
            }
            b.count++;
            b.distance += ex.getTotalDistance() / 10.0;
            b.durationMin += ex.getTotalDuration() / 60.0;
            b.calories += ex.getTotalCalories();
        }

        return map;
    }

    private static class Bucket {
        String displayLabel;
        int count = 0;
        double distance = 0;
        double durationMin = 0;
        double calories = 0;

        Bucket(String displayLabel) {
            this.displayLabel = displayLabel;
        }
    }
}
