package com.futsch1.medtimer.statistics;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.Insets;
import com.androidplot.ui.TableOrder;
import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYLegendWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.helpers.TimeHelper;

import java.text.DecimalFormat;
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


        medicinesPerDayChart.setRangeLowerBoundary(0, BoundaryMode.FIXED);

        setupBottomLine();
        setupLeftLine();
        setupNoBackgrounds();
        setupLegend();
    }

    private void setupBottomLine() {
        XYGraphWidget.LineLabelStyle bottomLine = medicinesPerDayChart.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM);
        bottomLine.setFormat(new DaysSinceEpochFormat());
        bottomLine.getPaint().setTextSize(chartHelper.dpToPx(10.0f));
        bottomLine.getPaint().setTextAlign(Paint.Align.CENTER);
        bottomLine.getPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorOnSurface));
    }

    private void setupLeftLine() {
        XYGraphWidget.LineLabelStyle leftLine =
                medicinesPerDayChart.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT);
        leftLine.setFormat(new DecimalFormat("#"));
        leftLine.getPaint().setTextSize(chartHelper.dpToPx(10.0f));
        leftLine.getPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorOnSurface));
    }

    private void setupNoBackgrounds() {
        medicinesPerDayChart.setBackgroundPaint(null);
        medicinesPerDayChart.getGraph().setRangeGridLinePaint(null);
        medicinesPerDayChart.getGraph().setDomainGridLinePaint(null);
        medicinesPerDayChart.getGraph().setBackgroundPaint(null);
        medicinesPerDayChart.getGraph().setGridBackgroundPaint(null);
        medicinesPerDayChart.getGraph().setDomainSubGridLinePaint(null);
        medicinesPerDayChart.getGraph().setRangeSubGridLinePaint(null);
        medicinesPerDayChart.getGraph().setDomainOriginLinePaint(null);
        medicinesPerDayChart.getGraph().setRangeOriginLinePaint(null);
    }

    private void setupLegend() {
        XYLegendWidget legend = medicinesPerDayChart.getLegend();
        legend.getTextPaint().setColor(chartHelper.getColor(com.google.android.material.R.attr.colorOnSurface));
        legend.getTextPaint().setTextSize(chartHelper.dpToPx(10.0f));
        legend.setTableModel(new DynamicTableModel(2, 2, TableOrder.ROW_MAJOR));
        legend.setVisible(true);
    }

    public void updateData(List<XYSeries> series) {
        medicinesPerDayChart.clear();
        colorIndex = 0;

        for (XYSeries xySeries : series) {
            medicinesPerDayChart.addSeries(xySeries, getFormatter(xySeries.getTitle()));
        }
        Number maxRange = calculateMaxRange(series);
        medicinesPerDayChart.setRangeUpperBoundary(maxRange, BoundaryMode.FIXED);
        medicinesPerDayChart.setRangeStep(StepMode.INCREMENT_BY_VAL, 1.0f);

        long minDomain = calculateMinDomain(series);
        long maxDomain = calculateMaxDomain(series);
        if (maxDomain == minDomain) {
            minDomain -= 1;
        }
        long numDomains = (maxDomain - minDomain + 1);
        medicinesPerDayChart.setDomainStep(StepMode.INCREMENT_BY_VAL, Math.ceil(numDomains / 7.0f));
        medicinesPerDayChart.setDomainBoundaries(minDomain, maxDomain, BoundaryMode.FIXED);

        medicinesPerDayChart.getLegend().setTableModel(new DynamicTableModel(Math.max(3, series.size() / 3), 3, TableOrder.ROW_MAJOR));

        setupRenderer(numDomains);
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

    private long calculateMinDomain(List<XYSeries> series) {
        if (series.isEmpty()) {
            return LocalDate.now().toEpochDay() - 1;
        } else {
            return series.get(0).getX(0).longValue();
        }
    }

    private long calculateMaxDomain(List<XYSeries> series) {
        if (series.isEmpty()) {
            return LocalDate.now().toEpochDay();
        } else {
            XYSeries first = series.get(0);
            return first.getX(first.size() - 1).longValue();
        }
    }

    private void setupRenderer(long numDomains) {
        // The space between each bar and next to the outer bars is half bar width.
        // o we have numDomainsBar bars, numDomains - 1 spaces + 2 outer bars
        float numBars = numDomains + (numDomains - 1 + 2) / 2.0f;
        float barWidth = medicinesPerDayChart.getGraph().getWidgetDimensions().paddedRect.width() / numBars;

        medicinesPerDayChart.getGraph().setGridInsets(new Insets(0, 0, barWidth, barWidth));

        MedicinePerDayChartRenderer renderer = medicinesPerDayChart.getRenderer(MedicinePerDayChartRenderer.class);
        renderer.setBarGroupWidth(BarRenderer.BarGroupWidthMode.FIXED_WIDTH, barWidth);
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
