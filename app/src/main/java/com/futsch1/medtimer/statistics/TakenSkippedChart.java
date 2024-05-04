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
        pieChart.addSegment(segmentTaken, getTakenFormatter());
        pieChart.addSegment(segmentSkipped, getSkippedFormatter());
        pieChart.setPlotPaddingTop(chartHelper.dpToPx(5.0f));
        PieRenderer renderer = pieChart.getRenderer(PieRenderer.class);
        renderer.setDonutSize(0.0f, PieRenderer.DonutMode.PERCENT);
        pieChart.getBackgroundPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorSurface));
        pieChart.getTitle().getLabelPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorOnSurface));
    }

    SegmentFormatter getTakenFormatter() {
        SegmentFormatter formatter = new SegmentFormatter(chartHelper.getColor(com.google.android.material.R.attr.colorPrimary));
        formatter.getLabelPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorOnPrimary));
        return formatter;
    }

    private SegmentFormatter getSkippedFormatter() {
        SegmentFormatter formatter = new SegmentFormatter(chartHelper.getColor(com.google.android.material.R.attr.colorSecondary));
        formatter.getLabelPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorOnSecondary));
        return formatter;
    }

    public void updateData(long taken, long skipped, int days) {
        if (days != 0) {
            String title = context.getString(R.string.last_n_days, days);
            pieChart.setTitle(title);
        } else {
            String title = context.getString(R.string.total);
            pieChart.setTitle(title);
        }
        segmentTaken.setValue(taken);
        if (taken > 0) {
            segmentTaken.setTitle(context.getString(R.string.taken) + ": " +
                    String.format(Locale.US, "%d%%", 100 * taken / (taken + skipped)));
        }
        segmentSkipped.setValue(skipped);
        if (skipped > 0) {
            segmentSkipped.setTitle(context.getString(R.string.skipped) + ": " +
                    String.format(Locale.US, "%d%%", 100 * skipped / (taken + skipped)));
        }
        pieChart.redraw();
    }
}
