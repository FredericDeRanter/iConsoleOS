package eu.le_tian.iConsoleOS.ui.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import androidx.cardview.widget.CardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import eu.le_tian.iConsoleOS.R;
import eu.le_tian.iConsoleOS.data.Exercise;
import eu.le_tian.iConsoleOS.databinding.FragmentAnalyticsBinding;
import eu.le_tian.iConsoleOS.ui.IConsoleViewModel;
import eu.le_tian.iConsoleOS.ui.analytics.ProgressStats.BestEffort;
import eu.le_tian.iConsoleOS.ui.analytics.ProgressStats.Metric;
import eu.le_tian.iConsoleOS.ui.analytics.ProgressStats.Period;
import eu.le_tian.iConsoleOS.ui.analytics.ProgressStats.Totals;

/**
 * Progress screen: totals and duration-weighted averages per week, month or ride, with a trend
 * line, summary cards and personal bests. The numbers come from {@link ProgressStats}.
 */
public class AnalyticsFragment extends Fragment {

    private static final String[] TIMEFRAMES = {"Last month", "Last 3 months", "Last 6 months", "Last 12 months", "All time"};
    private static final String[] TIMEFRAME_SHORT = {"month", "3 months", "6 months", "12 months", ""};
    private static final String[] GROUPINGS = {"Per week", "Per month", "Per ride"};
    private static final int PER_MONTH = 1, PER_RIDE = 2;
    /** Order of the metric spinner. */
    private static final Metric[] METRICS = {
            Metric.AVG_POWER, Metric.NORM_POWER, Metric.ENERGY, Metric.DISTANCE, Metric.DURATION,
            Metric.AVG_SPEED, Metric.AVG_RPM, Metric.AVG_HR, Metric.CALORIES, Metric.RIDES};
    private static final String ALL_PROFILES = "All profiles";
    private static final int RIDE_TREND = 5;

    private FragmentAnalyticsBinding binding;
    private IConsoleViewModel viewModel;
    private List<Exercise> allExercises = new ArrayList<>();
    private final List<String> profiles = new ArrayList<>();
    private ArrayAdapter<String> profileAdapter;

    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEE d MMM yyyy HH:mm", Locale.getDefault());
    private final SimpleDateFormat shortDayFormat = new SimpleDateFormat("d MMM yy", Locale.getDefault());

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
        styleChart(binding.periodChart);
        styleChart(binding.rideChart);
        binding.periodChart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE});

        OnChartValueSelectedListener onSelect = new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                showSelection(e.getData());
            }

            @Override
            public void onNothingSelected() {
                if (binding != null)
                    binding.tvSelection.setText("");
            }
        };
        binding.periodChart.setOnChartValueSelectedListener(onSelect);
        binding.rideChart.setOnChartValueSelectedListener(onSelect);

        viewModel.getAllExercisesChronologicalLive().observe(getViewLifecycleOwner(), exercises -> {
            allExercises = exercises != null ? exercises : new ArrayList<>();
            updateProfiles();
            update();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupSpinners() {
        setItems(binding.spinnerTimeframe, TIMEFRAMES);
        binding.spinnerTimeframe.setSelection(1);
        setItems(binding.spinnerGrouping, GROUPINGS);
        String[] metricNames = new String[METRICS.length];
        for (int i = 0; i < METRICS.length; i++)
            metricNames[i] = METRICS[i].title + " (" + METRICS[i].unit + ")";
        setItems(binding.spinnerMetric, metricNames);

        profileAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, profiles);
        profileAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerProfile.setAdapter(profileAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                update();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        binding.spinnerTimeframe.setOnItemSelectedListener(listener);
        binding.spinnerGrouping.setOnItemSelectedListener(listener);
        binding.spinnerMetric.setOnItemSelectedListener(listener);
        binding.spinnerProfile.setOnItemSelectedListener(listener);
    }

    private void setItems(Spinner spinner, String[] items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(a);
    }

    /** Profiles that have rides, most used first; keeps the current selection. */
    private void updateProfiles() {
        String selected = (String) binding.spinnerProfile.getSelectedItem();
        Map<String, Integer> counts = new HashMap<>();
        for (Exercise e : allExercises) {
            if (e.getExProfileName() != null)
                counts.merge(e.getExProfileName(), 1, Integer::sum);
        }
        List<String> names = new ArrayList<>(counts.keySet());
        names.sort((a, b) -> Integer.compare(counts.get(b), counts.get(a)));
        profiles.clear();
        profiles.add(ALL_PROFILES);
        profiles.addAll(names);
        profileAdapter.notifyDataSetChanged();
        int pos = selected != null ? profiles.indexOf(selected) : 0;
        binding.spinnerProfile.setSelection(Math.max(pos, 0));
    }

    private void update() {
        if (binding == null)
            return;
        int timeframe = binding.spinnerTimeframe.getSelectedItemPosition();
        int grouping = binding.spinnerGrouping.getSelectedItemPosition();
        Metric metric = METRICS[Math.max(binding.spinnerMetric.getSelectedItemPosition(), 0)];
        String profileItem = (String) binding.spinnerProfile.getSelectedItem();
        String profile = profileItem == null || ALL_PROFILES.equals(profileItem) ? null : profileItem;

        long now = System.currentTimeMillis();
        long from = cutoff(timeframe, now);
        List<Exercise> rides = ProgressStats.filter(allExercises, from, now, profile);

        Totals totals = ProgressStats.totals(rides, 0, Long.MAX_VALUE);
        Totals previous = null;
        if (from > 0) {
            List<Exercise> before = ProgressStats.filter(allExercises, from - (now - from), from - 1, profile);
            previous = ProgressStats.totals(before, 0, Long.MAX_VALUE);
        }
        showCards(totals, previous, metric, rides, timeframe);

        binding.tvSelection.setText("Tap a bar or point for details. Pinch to zoom, drag to scroll.");
        if (grouping == PER_RIDE) {
            binding.periodChart.setVisibility(View.GONE);
            binding.rideChart.setVisibility(View.VISIBLE);
            showRides(rides, metric);
        } else {
            binding.rideChart.setVisibility(View.GONE);
            binding.periodChart.setVisibility(View.VISIBLE);
            showPeriods(ProgressStats.periods(rides, from, now, grouping == PER_MONTH, TimeZone.getDefault()), metric);
        }

        int withNP = 0;
        for (Exercise e : rides) {
            if (e.getNormPower() > 0)
                withNP++;
        }
        binding.tvFootnote.setText(String.format(Locale.US,
                "Averages over several rides are weighted by ride duration. Normalized Power and best "
                        + "efforts are known for %d of %d rides here: they need the ride's samples, so only "
                        + "rides recorded from November 2026 on (and the few older ones that kept their samples).",
                withNP, rides.size()));
    }

    private static long cutoff(int timeframe, long now) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        switch (timeframe) {
            case 0: c.add(Calendar.MONTH, -1); break;
            case 1: c.add(Calendar.MONTH, -3); break;
            case 2: c.add(Calendar.MONTH, -6); break;
            case 3: c.add(Calendar.YEAR, -1); break;
            default: return 0;
        }
        return c.getTimeInMillis();
    }

    // ---- summary cards ----

    private void showCards(Totals t, @Nullable Totals previous, Metric metric, List<Exercise> rides, int timeframe) {
        binding.summaryRow.removeAllViews();
        binding.intensityRow.removeAllViews();
        binding.bestsRow.removeAllViews();

        addCard(binding.summaryRow, "Workouts", String.valueOf(t.rides));
        addCard(binding.summaryRow, "Duration", formatMinutes(t.minutes()));
        addCard(binding.summaryRow, "Distance", String.format(Locale.US, "%.1f km", t.distanceKm));
        addCard(binding.summaryRow, "Energy", String.format(Locale.US, "%.0f kJ", t.energyKJ));

        addCard(binding.intensityRow, "Avg power", format(t.avgPower(), Metric.AVG_POWER));
        int withNP = 0;
        for (Exercise e : rides) {
            if (e.getNormPower() > 0)
                withNP++;
        }
        addCard(binding.intensityRow, withNP == t.rides ? "Normalized Power" : "NP (" + withNP + " of " + t.rides + " rides)",
                format(t.normPower(), Metric.NORM_POWER));
        addCard(binding.intensityRow, "Avg cadence", format(t.avgRPM(), Metric.AVG_RPM));
        String vsLabel = metric.title + " vs previous " + TIMEFRAME_SHORT[timeframe];
        String vs = "–";
        if (previous != null) {
            double change = ProgressStats.change(t.value(metric), previous.value(metric));
            if (!Double.isNaN(change))
                vs = String.format(Locale.US, "%+.0f%%", change * 100);
        } else {
            vsLabel = metric.title + " vs previous";
        }
        addCard(binding.intensityRow, vsLabel, vs);

        for (BestEffort b : new BestEffort[]{BestEffort.MIN_5, BestEffort.MIN_20, BestEffort.MIN_60}) {
            Exercise e = ProgressStats.best(rides, b);
            addCard(binding.bestsRow,
                    "Best " + b.title + (e == null ? "" : ", " + shortDayFormat.format(new Date(e.getExStartDateTime()))),
                    e == null ? "–" : String.format(Locale.US, "%.0f W", b.of(e) / 10.0));
        }
        addCard(binding.bestsRow, "Longest ride", t.rides > 0 ? formatMinutes(t.longestRideMin) : "–");
    }

    private void addCard(LinearLayout row, String label, String value) {
        CardView card = new CardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        int m = dp(3);
        lp.setMargins(m, 0, m, 0);
        card.setLayoutParams(lp);
        card.setRadius(dp(6));
        card.setCardElevation(dp(2));

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(8), dp(6), dp(8), dp(6));

        TextView l = new TextView(requireContext());
        l.setText(label);
        l.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        TextView v = new TextView(requireContext());
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        v.setTypeface(v.getTypeface(), android.graphics.Typeface.BOLD);
        v.setGravity(Gravity.START);
        inner.addView(l);
        inner.addView(v);
        card.addView(inner);
        row.addView(card);
    }

    // ---- charts ----

    private void styleChart(BarLineChartBase<?> chart) {
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setNoDataText("No rides in this timeframe");
        chart.setScaleYEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);
        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setDrawGridLines(false);
        chart.getLegend().setWordWrapEnabled(true);
    }

    private void showPeriods(List<Period> periods, Metric metric) {
        CombinedChart chart = binding.periodChart;
        chart.clear();
        chart.fitScreen();
        binding.tvChartTitle.setText(metric.title + " (" + metric.unit + ") per "
                + (binding.spinnerGrouping.getSelectedItemPosition() == PER_MONTH ? "month" : "week"));
        if (periods.isEmpty())
            return;

        double[] values = ProgressStats.values(periods, metric);
        double[] trend = ProgressStats.trend(values, ProgressStats.TREND_PERIODS);
        List<BarEntry> bars = new ArrayList<>();
        String[] labels = new String[periods.size()];
        for (int i = 0; i < periods.size(); i++) {
            labels[i] = periods.get(i).label;
            if (!Double.isNaN(values[i]))
                bars.add(new BarEntry(i, (float) values[i], periods.get(i)));
        }
        if (bars.isEmpty())
            return;

        int primary = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        int accent = ContextCompat.getColor(requireContext(), R.color.colorAccent);

        BarDataSet barSet = new BarDataSet(bars, metric.title);
        barSet.setColor(primary);
        barSet.setHighLightColor(accent);
        barSet.setDrawValues(false);
        BarData barData = new BarData(barSet);
        barData.setBarWidth(0.75f);

        CombinedData data = new CombinedData();
        data.setData(barData);
        List<ILineDataSet> trendSets = trendSegments(trend, periods, ProgressStats.TREND_PERIODS + "-"
                + (binding.spinnerGrouping.getSelectedItemPosition() == PER_MONTH ? "month" : "week") + " average", accent);
        if (!trendSets.isEmpty())
            data.setData(new LineData(trendSets));

        XAxis x = chart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setAxisMinimum(-0.5f);
        x.setAxisMaximum(periods.size() - 0.5f);
        x.setLabelRotationAngle(periods.size() > 8 ? -45f : 0f);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.setData(data);
        chart.setVisibleXRangeMaximum(26f);   // about half a year of weeks; drag for more
        chart.moveViewToX(periods.size());
        chart.invalidate();
    }

    private void showRides(List<Exercise> rides, Metric metric) {
        LineChart chart = binding.rideChart;
        chart.clear();
        chart.fitScreen();
        binding.tvChartTitle.setText(metric.title + " (" + metric.unit + ") per ride");
        if (metric == Metric.RIDES) {
            chart.setNoDataText("Pick another metric for the per-ride view");
            return;
        }
        chart.setNoDataText("No rides in this timeframe");
        if (rides.isEmpty())
            return;

        int primary = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        int accent = ContextCompat.getColor(requireContext(), R.color.colorAccent);
        int third = ContextCompat.getColor(requireContext(), R.color.colorButton);

        String[] labels = new String[rides.size()];
        double[] values = new double[rides.size()];
        List<Entry> points = new ArrayList<>();
        List<Entry> np = new ArrayList<>();
        for (int i = 0; i < rides.size(); i++) {
            Exercise e = rides.get(i);
            labels[i] = shortDayFormat.format(new Date(e.getExStartDateTime()));
            Totals one = new Totals();
            one.add(e);
            values[i] = one.value(metric);
            if (!Double.isNaN(values[i]) && values[i] > 0)
                points.add(new Entry(i, (float) values[i], e));
            else
                values[i] = Double.NaN;
            if (metric == Metric.AVG_POWER && e.getNormPower() > 0)
                np.add(new Entry(i, e.getNormPower() / 10f, e));
        }
        if (points.isEmpty()) {
            chart.setNoDataText("No ride in this timeframe has " + metric.title.toLowerCase(Locale.US));
            return;
        }

        List<ILineDataSet> sets = new ArrayList<>();
        LineDataSet main = lineSet(points, metric.title, primary);
        main.setDrawCircles(true);
        main.setCircleColor(primary);
        main.setCircleRadius(3f);
        main.setDrawCircleHole(false);
        main.setLineWidth(1f);
        main.enableDashedLine(6f, 6f, 0f);
        sets.add(main);
        if (!np.isEmpty()) {
            LineDataSet npSet = lineSet(np, "Normalized Power", third);
            npSet.setDrawCircles(true);
            npSet.setCircleColor(third);
            npSet.setCircleRadius(3f);
            npSet.setDrawCircleHole(false);
            npSet.setLineWidth(1f);
            sets.add(npSet);
        }
        sets.addAll(trendSegments(ProgressStats.trend(values, RIDE_TREND), rides, RIDE_TREND + "-ride average", accent));

        XAxis x = chart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setAxisMinimum(-0.5f);
        x.setAxisMaximum(rides.size() - 0.5f);
        x.setLabelRotationAngle(-45f);
        chart.getAxisLeft().resetAxisMinimum();
        chart.setData(new LineData(sets));
        chart.setVisibleXRangeMaximum(40f);
        chart.moveViewToX(rides.size());
        chart.invalidate();
    }

    /**
     * The trend line as one data set per run of known values, so it isn't drawn straight across
     * a stretch without rides. Only the first segment appears in the legend.
     */
    private static List<ILineDataSet> trendSegments(double[] trend, List<?> items, String label, int color) {
        List<ILineDataSet> sets = new ArrayList<>();
        List<Entry> run = new ArrayList<>();
        for (int i = 0; i <= trend.length; i++) {
            if (i < trend.length && !Double.isNaN(trend[i])) {
                run.add(new Entry(i, (float) trend[i], items.get(i)));
                continue;
            }
            if (run.size() > 1) {
                LineDataSet set = lineSet(run, sets.isEmpty() ? label : null, color);
                set.setHighlightEnabled(false);
                if (!sets.isEmpty())
                    set.setForm(Legend.LegendForm.NONE);
                sets.add(set);
            }
            run = new ArrayList<>();
        }
        return sets;
    }

    private static LineDataSet lineSet(List<Entry> entries, String label, int color) {
        LineDataSet set = new LineDataSet(entries, label);
        set.setColor(color);
        set.setLineWidth(2.5f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setHighLightColor(Color.GRAY);
        return set;
    }

    // ---- tap details ----

    private void showSelection(Object data) {
        if (binding == null)
            return;
        if (data instanceof Period) {
            Period p = (Period) data;
            binding.tvSelection.setText(String.format(Locale.US,
                    "%s: %d ride%s, %s, %.1f km, %.0f kJ\nAvg power %s, NP %s, cadence %s, speed %s",
                    p.label, p.rides, p.rides == 1 ? "" : "s", formatMinutes(p.minutes()), p.distanceKm, p.energyKJ,
                    format(p.avgPower(), Metric.AVG_POWER), format(p.normPower(), Metric.NORM_POWER),
                    format(p.avgRPM(), Metric.AVG_RPM), format(p.avgSpeed(), Metric.AVG_SPEED)));
        } else if (data instanceof Exercise) {
            Exercise e = (Exercise) data;
            StringBuilder bests = new StringBuilder();
            for (BestEffort b : new BestEffort[]{BestEffort.MIN_5, BestEffort.MIN_20, BestEffort.MIN_60}) {
                if (b.of(e) > 0)
                    bests.append(String.format(Locale.US, ", best %s %.0f W", b.title, b.of(e) / 10.0));
            }
            binding.tvSelection.setText(String.format(Locale.US,
                    "%s – %s: %s, %.1f km\nAvg power %.1f W%s%s, cadence %d rpm",
                    dayFormat.format(new Date(e.getExStartDateTime())), e.getExProfileName(),
                    formatMinutes(e.getTotalDuration() / 60.0), e.getTotalDistance() / 10.0,
                    e.getAvgPower() / 10.0,
                    e.getNormPower() > 0 ? String.format(Locale.US, ", NP %.0f W", e.getNormPower() / 10.0) : "",
                    bests, e.getAvgRPM()));
        }
    }

    // ---- formatting ----

    private static String format(double v, Metric m) {
        if (Double.isNaN(v))
            return "–";
        String digits = m == Metric.DISTANCE || m == Metric.AVG_SPEED ? "%.1f" : "%.0f";
        return String.format(Locale.US, digits + " %s", v, m.unit);
    }

    private static String formatMinutes(double minutes) {
        long m = Math.round(minutes);
        return m >= 60 ? String.format(Locale.US, "%dh %02dm", m / 60, m % 60) : m + " min";
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
