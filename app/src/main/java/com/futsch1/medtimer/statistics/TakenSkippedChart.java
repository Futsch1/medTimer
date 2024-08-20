package com.futsch1.medtimer.statistics;

import android.content.Context;

import com.androidplot.pie.PieChart;
import com.androidplot.pie.PieRenderer;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.futsch1.medtimer.R;

import java.util.Locale;

public class TakenSkippedChart {
    private final PieChart pieChart;
    private final Context context;
    private final Segment segmentTaken;
    private final Segment segmentSkipped;
    private final ChartHelper chartHelper;

    public TakenSkippedChart(PieChart pieChart, Context context) {
        this.chartHelper = new ChartHelper(context);
        this.pieChart = pieChart;
        this.context = context;
        this.segmentTaken = new Segment(context.getString(R.string.taken), 0);
        this.segmentSkipped = new Segment(context.getString(R.string.skipped), 0);
        pieChart.addSegment(segmentTaken, getFormatter(com.google.android.material.R.attr.colorPrimary, com.google.android.material.R.attr.colorOnPrimary));
        pieChart.addSegment(segmentSkipped, getFormatter(com.google.android.material.R.attr.colorSecondary, com.google.android.material.R.attr.colorOnSecondary));
        pieChart.setPlotPaddingTop(chartHelper.dpToPx(5.0f));
        PieRenderer renderer = pieChart.getRenderer(PieRenderer.class);
        renderer.setDonutSize(0.0f, PieRenderer.DonutMode.PERCENT);
        pieChart.getBackgroundPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorSurface));
        pieChart.getTitle().getLabelPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorOnSurface));
    }

    SegmentFormatter getFormatter(int colorSegment, int colorText) {
        SegmentFormatter formatter = new SegmentFormatter(chartHelper.getColor(colorSegment));
        formatter.getLabelPaint().setColor(chartHelper.getColor(colorText));
        return formatter;
    }

    public void updateData(long taken, long skipped, int days) {
        String title;
        if (days != 0) {
            title = context.getResources().getQuantityString(R.plurals.last_n_days, days, days);
        } else {
            title = context.getString(R.string.total);
        }
        pieChart.setTitle(title);

        setupSegment(segmentTaken, taken, skipped, R.string.taken);
        setupSegment(segmentSkipped, skipped, taken, R.string.skipped);
        pieChart.redraw();
    }

    private void setupSegment(Segment segment, long selfValue, long otherValue, int stringId) {
        segment.setValue(selfValue);
        if (selfValue > 0) {
            segment.setTitle(context.getString(stringId) + ": " +
                    String.format(Locale.US, "%d%%", 100 * selfValue / (selfValue + otherValue)));
        } else {
            segment.setTitle("");
        }
    }
}
