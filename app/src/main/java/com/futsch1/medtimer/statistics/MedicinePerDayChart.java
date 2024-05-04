package com.futsch1.medtimer.statistics;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYLegendWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.helpers.TimeHelper;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.util.List;

public class MedicinePerDayChart {
    private final XYPlot medicinesPerDayChart;
    private final List<MedicineWithReminders> medicines;
    private final ChartHelper chartHelper;
    private int colorIndex;

    public MedicinePerDayChart(XYPlot medicinesPerDayChart, Context context, List<MedicineWithReminders> medicines) {
        this.medicinesPerDayChart = medicinesPerDayChart;
        this.chartHelper = new ChartHelper(context);
        this.medicines = medicines;

        XYGraphWidget.LineLabelStyle bottomLine = medicinesPerDayChart.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM);
        bottomLine.setFormat(new DaysSinceEpochFormat());
        bottomLine.getPaint().setTextSize(chartHelper.dpToPx(10.0f));
        bottomLine.getPaint().setTextAlign(Paint.Align.CENTER);
        bottomLine.getPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorOnSurface));

        medicinesPerDayChart.getBackgroundPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorSurface));

        medicinesPerDayChart.getGraph().getGridBackgroundPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorSurface));
        medicinesPerDayChart.getGraph().setMarginBottom(chartHelper.dpToPx(10.0f));
        medicinesPerDayChart.getGraph().setPadding(chartHelper.dpToPx(10.0f), chartHelper.dpToPx(10.0f), chartHelper.dpToPx(10.0f), chartHelper.dpToPx(10.0f));

        medicinesPerDayChart.setRangeLowerBoundary(0, BoundaryMode.FIXED);

        XYLegendWidget legend = medicinesPerDayChart.getLegend();
        legend.setMarginBottom(chartHelper.dpToPx(10.0f));
        legend.getTextPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorOnSurface));
        legend.getTextPaint().setTextSize(chartHelper.dpToPx(10.0f));
    }

    public void updateData(List<XYSeries> series) {
        medicinesPerDayChart.clear();
        colorIndex = 0;

        for (XYSeries xySeries : series) {
            medicinesPerDayChart.addSeries(xySeries, getFormatter(xySeries.getTitle()));
        }
        medicinesPerDayChart.setRangeUpperBoundary(calculateMaxRange(series), BoundaryMode.FIXED);
        medicinesPerDayChart.setDomainBoundaries(calculateMinDomain(series), calculateMaxDomain(series), BoundaryMode.FIXED);
        setupRenderer();
        medicinesPerDayChart.redraw();
    }

    private MedicinePerDayChartFormatter getFormatter(String title) {
        MedicinePerDayChartFormatter formatter = new MedicinePerDayChartFormatter();
        formatter.getFillPaint().setColor(getColor(title));
        formatter.getBorderPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorOnPrimary));
        return formatter;
    }

    private Number calculateMaxRange(List<XYSeries> series) {
        long max = 0;
        if (series.isEmpty()) {
            return max;
        }
        for (int x = 0; x < series.get(0).size(); x++) {
            long sum = 0;

            for (XYSeries xySeries : series) {
                sum += xySeries.getY(x).longValue();
            }
            if (sum > max) {
                max = sum;
            }
        }
        return max;
    }

    private Number calculateMinDomain(List<XYSeries> series) {
        if (series.isEmpty()) {
            return LocalDate.now().toEpochDay() - 1;
        } else {
            return series.get(0).getX(0).intValue() - 1;
        }
    }

    private Number calculateMaxDomain(List<XYSeries> series) {
        if (series.isEmpty()) {
            return LocalDate.now().toEpochDay();
        } else {
            XYSeries first = series.get(0);
            return first.getX(first.size() - 1).intValue() + 1;
        }
    }

    private void setupRenderer() {
        MedicinePerDayChartRenderer renderer = medicinesPerDayChart.getRenderer(MedicinePerDayChartRenderer.class);
        renderer.setBarGroupWidth(BarRenderer.BarGroupWidthMode.FIXED_GAP, chartHelper.dpToPx(20.0f));
        renderer.setBarOrientation(BarRenderer.BarOrientation.STACKED);
    }

    private int getColor(String title) {
        for (MedicineWithReminders medicine : medicines) {
            if (medicine.medicine.name.equals(title) && medicine.medicine.useColor) {
                return medicine.medicine.color;
            }
        }
        String[] colors = {"#003f5c", "#2f4b7c", "#665191", "#a05195", "#d45087", "#f95d6a",
                "#ff7c43", "#ffa600", "#004c6d", "#295d7d", "#436f8e", "#5b829f", "#7295b0",
                "#89a8c2", "#a1bcd4", "#b8d0e6", "#d0e5f8"};

        return Color.parseColor(colors[colorIndex++ % colors.length]);
    }

    private static class DaysSinceEpochFormat extends NumberFormat {
        @NonNull
        @Override
        public StringBuffer format(double value, @NonNull StringBuffer buffer,
                                   @NonNull FieldPosition field) {
            return buffer.append(TimeHelper.daysSinceEpochToDateString((long) value));
        }

        @NonNull
        @Override
        public StringBuffer format(long value, @NonNull StringBuffer buffer,
                                   @NonNull FieldPosition field) {
            throw new UnsupportedOperationException("Not yet implemented.");
        }

        @Override
        public Number parse(@NonNull String string, @NonNull ParsePosition position) {
            throw new UnsupportedOperationException("Not yet implemented.");

        }
    }

    class MedicinePerDayChartFormatter extends BarFormatter {
        @Override
        public Class<? extends BarRenderer<?>> getRendererClass() {
            return MedicinePerDayChartRenderer.class;
        }

        @Override
        public BarRenderer<?> doGetRendererInstance(XYPlot plot) {
            return new MedicinePerDayChartRenderer(plot);
        }
    }

    class MedicinePerDayChartRenderer extends BarRenderer<MedicinePerDayChartFormatter> {
        public MedicinePerDayChartRenderer(XYPlot plot) {
            super(plot);
        }
    }
}
