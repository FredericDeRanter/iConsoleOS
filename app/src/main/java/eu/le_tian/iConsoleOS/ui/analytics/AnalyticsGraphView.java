package eu.le_tian.iConsoleOS.ui.analytics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnalyticsGraphView extends View {

    public static class DataPoint {
        public String label;
        public double value;
        public int workoutCount;
        public double totalDistance;
        public double totalDurationMin;
        public double totalCalories;

        public DataPoint(String label, double value, int workoutCount, double totalDistance, double totalDurationMin, double totalCalories) {
            this.label = label;
            this.value = value;
            this.workoutCount = workoutCount;
            this.totalDistance = totalDistance;
            this.totalDurationMin = totalDurationMin;
            this.totalCalories = totalCalories;
        }
    }

    private List<DataPoint> dataPoints = new ArrayList<>();
    private String unitLabel = "";
    private int selectedIndex = -1;

    private Paint gridPaint;
    private Paint axisPaint;
    private Paint barPaint;
    private Paint barSelectedPaint;
    private Paint textPaint;
    private Paint labelPaint;
    private Paint tooltipBgPaint;
    private Paint tooltipTextPaint;

    public AnalyticsGraphView(Context context) {
        super(context);
        init();
    }

    public AnalyticsGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnalyticsGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1.5f);
        gridPaint.setColor(Color.LTGRAY);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(3f);
        axisPaint.setColor(Color.DKGRAY);

        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(Color.parseColor("#00796B")); // Teal primary

        barSelectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barSelectedPaint.setStyle(Paint.Style.FILL);
        barSelectedPaint.setColor(Color.parseColor("#FF6F00")); // Amber selected

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(32f);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(28f);
        labelPaint.setColor(Color.DKGRAY);
        labelPaint.setTextAlign(Paint.Align.RIGHT);

        tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipBgPaint.setStyle(Paint.Style.FILL);
        tooltipBgPaint.setColor(Color.argb(220, 33, 33, 33));

        tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipTextPaint.setTextSize(30f);
        tooltipTextPaint.setColor(Color.WHITE);
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setDataPoints(List<DataPoint> dataPoints, String unitLabel) {
        this.dataPoints = dataPoints != null ? dataPoints : new ArrayList<>();
        this.unitLabel = unitLabel != null ? unitLabel : "";
        this.selectedIndex = -1;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);

        int width = getWidth();
        int height = getHeight();

        int paddingLeft = 110;
        int paddingBottom = 90;
        int paddingTop = 60;
        int paddingRight = 40;

        int graphWidth = width - paddingLeft - paddingRight;
        int graphHeight = height - paddingTop - paddingBottom;

        if (dataPoints.isEmpty() || graphWidth <= 0 || graphHeight <= 0) {
            canvas.drawText("No workout data recorded for this timeframe", width / 2f, height / 2f, textPaint);
            return;
        }

        // Find max value
        double maxVal = 0;
        for (DataPoint dp : dataPoints) {
            if (dp.value > maxVal) {
                maxVal = dp.value;
            }
        }
        if (maxVal <= 0) {
            maxVal = 10.0;
        } else {
            maxVal = Math.ceil(maxVal * 1.15); // Add 15% headroom
        }

        // Draw Y-axis grid & labels
        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            float yPos = paddingTop + graphHeight - (i * (graphHeight / (float) steps));
            double yVal = (maxVal / steps) * i;

            canvas.drawLine(paddingLeft, yPos, paddingLeft + graphWidth, yPos, (i == 0) ? axisPaint : gridPaint);

            String yLabel = String.format(Locale.US, yVal >= 100 ? "%.0f" : "%.1f", yVal);
            canvas.drawText(yLabel, paddingLeft - 15, yPos + 10, labelPaint);
        }

        // Draw unit label on top left of Y axis
        if (!unitLabel.isEmpty()) {
            canvas.drawText("(" + unitLabel + ")", paddingLeft - 15, paddingTop - 25, labelPaint);
        }

        // Draw X-axis
        canvas.drawLine(paddingLeft, paddingTop + graphHeight, paddingLeft + graphWidth, paddingTop + graphHeight, axisPaint);

        // Draw Bars
        int count = dataPoints.size();
        float totalSpacing = graphWidth * 0.2f;
        float barSpacing = totalSpacing / (count + 1);
        float barWidth = (graphWidth - totalSpacing) / count;
        if (barWidth > 120) barWidth = 120; // Cap bar width for clean visuals

        for (int i = 0; i < count; i++) {
            DataPoint dp = dataPoints.get(i);
            float left = paddingLeft + barSpacing + i * (barWidth + barSpacing);
            float right = left + barWidth;

            float barH = (float) ((dp.value / maxVal) * graphHeight);
            float top = paddingTop + graphHeight - barH;
            float bottom = paddingTop + graphHeight;

            RectF rect = new RectF(left, top, right, bottom);
            Paint p = (i == selectedIndex) ? barSelectedPaint : barPaint;
            canvas.drawRoundRect(rect, 8, 8, p);

            // Value text above bar (if space permits)
            if (dp.value > 0) {
                String valStr = String.format(Locale.US, dp.value >= 10 ? "%.0f" : "%.1f", dp.value);
                canvas.drawText(valStr, left + barWidth / 2f, Math.max(top - 10, paddingTop), textPaint);
            }

            // X-axis label below bar
            if (dp.label != null) {
                canvas.drawText(dp.label, left + barWidth / 2f, bottom + 45, textPaint);
            }
        }

        // Draw tooltip if a bar is selected
        if (selectedIndex >= 0 && selectedIndex < dataPoints.size()) {
            DataPoint sel = dataPoints.get(selectedIndex);
            String tip1 = String.format(Locale.US, "%s: %.1f %s (%d workouts)", sel.label, sel.value, unitLabel, sel.workoutCount);
            String tip2 = String.format(Locale.US, "Dist: %.1f km  |  Dur: %.0f min  |  Cal: %.0f kcal", sel.totalDistance, sel.totalDurationMin, sel.totalCalories);

            float tooltipW = Math.max(textPaint.measureText(tip1), textPaint.measureText(tip2)) + 40;
            float tooltipH = 100;
            float tooltipX = Math.min(Math.max(width / 2f - tooltipW / 2f, 20), width - tooltipW - 20);
            float tooltipY = paddingTop + 10;

            RectF tipRect = new RectF(tooltipX, tooltipY, tooltipX + tooltipW, tooltipY + tooltipH);
            canvas.drawRoundRect(tipRect, 16, 16, tooltipBgPaint);
            canvas.drawText(tip1, tooltipX + tooltipW / 2f, tooltipY + 40, tooltipTextPaint);
            canvas.drawText(tip2, tooltipX + tooltipW / 2f, tooltipY + 80, tooltipTextPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int width = getWidth();
            int height = getHeight();

            int paddingLeft = 110;
            int paddingBottom = 90;
            int paddingTop = 60;
            int paddingRight = 40;

            int graphWidth = width - paddingLeft - paddingRight;
            int count = dataPoints.size();
            if (count > 0 && graphWidth > 0) {
                float totalSpacing = graphWidth * 0.2f;
                float barSpacing = totalSpacing / (count + 1);
                float barWidth = (graphWidth - totalSpacing) / count;
                if (barWidth > 120) barWidth = 120;

                float x = event.getX();
                for (int i = 0; i < count; i++) {
                    float left = paddingLeft + barSpacing + i * (barWidth + barSpacing);
                    float right = left + barWidth;
                    if (x >= left && x <= right) {
                        selectedIndex = (selectedIndex == i) ? -1 : i;
                        invalidate();
                        return true;
                    }
                }
            }
            selectedIndex = -1;
            invalidate();
        }
        return super.onTouchEvent(event);
    }
}
