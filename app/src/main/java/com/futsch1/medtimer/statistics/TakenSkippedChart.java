package com.futsch1.medtimer.statistics;

import android.content.Context;

import com.androidplot.pie.PieChart;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.futsch1.medtimer.R;
import com.google.android.material.color.MaterialColors;

public class TakenSkippedChart {
    private final PieChart pieChart;
    private final Context context;
    private final Segment segmentTaken;
    private final Segment segmentSkipped;

    public TakenSkippedChart(PieChart pieChart, Context context) {
        this.pieChart = pieChart;
        this.context = context;
        this.segmentTaken = new Segment(context.getString(R.string.taken), 0);
        this.segmentSkipped = new Segment(context.getString(R.string.skipped), 0);
        pieChart.addSegment(segmentTaken, new SegmentFormatter(MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryContainer, "")));
        pieChart.addSegment(segmentSkipped, new SegmentFormatter(MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, "")));
    }

    public void updateData(long taken, long skipped, int days) {
        if (days != 0) {
            String title = context.getString(R.string.last_n_days, days);
            pieChart.setTitle(title);
        }
        segmentTaken.setValue(taken);
        segmentSkipped.setValue(skipped);
        pieChart.redraw();
    }
}
