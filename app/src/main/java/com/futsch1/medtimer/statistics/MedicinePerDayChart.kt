package com.futsch1.medtimer.statistics

import android.content.Context
import android.graphics.Paint
import androidx.core.graphics.toColorInt
import com.androidplot.ui.DynamicTableModel
import com.androidplot.ui.Insets
import com.androidplot.ui.TableOrder
import com.androidplot.xy.BarFormatter
import com.androidplot.xy.BarRenderer
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.SimpleXYSeries
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYGraphWidget
import com.androidplot.xy.XYPlot
import com.androidplot.xy.XYSeries
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.helpers.TimeHelper.daysSinceEpochToDateString
import com.futsch1.medtimer.helpers.dpToPx
import com.futsch1.medtimer.helpers.getMaterialColor
import com.google.android.material.R
import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.NumberFormat
import java.text.ParsePosition
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.max

class MedicinePerDayChart(
    private val medicinesPerDayChart: XYPlot,
    private val context: Context,
    private val medicines: MutableList<FullMedicine>
) {
    private var colorIndex = 0

    companion object {
        private val COLORS = intArrayOf(
            "#003f5c".toColorInt(),
            "#2f4b7c".toColorInt(),
            "#665191".toColorInt(),
            "#a05195".toColorInt(),
            "#d45087".toColorInt(),
            "#f95d6a".toColorInt(),
            "#ff7c43".toColorInt(),
            "#ffa600".toColorInt(),
            "#004c6d".toColorInt(),
            "#295d7d".toColorInt(),
            "#436f8e".toColorInt(),
            "#5b829f".toColorInt(),
            "#7295b0".toColorInt(),
            "#89a8c2".toColorInt(),
            "#a1bcd4".toColorInt(),
            "#b8d0e6".toColorInt(),
            "#d0e5f8".toColorInt()
        )
    }

    init {
        medicinesPerDayChart.setRangeLowerBoundary(0, BoundaryMode.FIXED)

        setupBottomLine()
        setupLeftLine()
        setupNoBackgrounds()
        setupLegend()
    }

    private fun setupBottomLine() {
        val bottomLine = medicinesPerDayChart.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM)
        bottomLine.format = DaysSinceEpochFormat()
        bottomLine.paint.textSize = context.resources.dpToPx(10.0f)
        bottomLine.paint.textAlign = Paint.Align.CENTER
        bottomLine.paint.setColor(context.getMaterialColor(R.attr.colorOnSurface))
    }

    private fun setupLeftLine() {
        val leftLine = medicinesPerDayChart.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT)
        leftLine.format = DecimalFormat("#")
        leftLine.paint.textSize = context.resources.dpToPx(10.0f)
        leftLine.paint.setColor(context.getMaterialColor(R.attr.colorOnSurface))
    }

    private fun setupNoBackgrounds() {
        medicinesPerDayChart.backgroundPaint = null
        medicinesPerDayChart.graph.rangeGridLinePaint = null
        medicinesPerDayChart.graph.domainGridLinePaint = null
        medicinesPerDayChart.graph.backgroundPaint = null
        medicinesPerDayChart.graph.gridBackgroundPaint = null
        medicinesPerDayChart.graph.domainSubGridLinePaint = null
        medicinesPerDayChart.graph.rangeSubGridLinePaint = null
        medicinesPerDayChart.graph.domainOriginLinePaint = null
        medicinesPerDayChart.graph.rangeOriginLinePaint = null
    }

    private fun setupLegend() {
        val legend = medicinesPerDayChart.legend
        legend.textPaint.setColor(context.getMaterialColor(R.attr.colorOnSurface))
        legend.textPaint.textSize = context.resources.dpToPx(10.0f)
        legend.setPadding(context.resources.dpToPx(4.0f), 0f, context.resources.dpToPx(4.0f), 0f)
    }

    fun updateData(series: MutableList<SimpleXYSeries>) {
        medicinesPerDayChart.clear()
        colorIndex = 0
        val numLegendColumns = max(3, ceil((series.size / 3.0)).toInt())
        var columnSize = medicinesPerDayChart.legend.widgetDimensions.paddedRect.width()
            .toInt() / numLegendColumns
        columnSize -= medicinesPerDayChart.legend.iconSize.width.getPixelValue(0.0f).toInt() + 2

        for (xySeries in series) {
            medicinesPerDayChart.addSeries(xySeries, getFormatter(xySeries.title))
            adjustTitleLength(xySeries, columnSize, medicinesPerDayChart.legend.textPaint)
        }
        val maxRange = calculateMaxRange(series)
        medicinesPerDayChart.setRangeUpperBoundary(maxRange, BoundaryMode.FIXED)
        medicinesPerDayChart.setRangeStep(StepMode.INCREMENT_BY_VAL, 1.0)

        var minDomain = calculateMinDomain(series)
        val maxDomain = calculateMaxDomain(series)
        if (maxDomain == minDomain) {
            minDomain -= 1
        }
        val numDomains = (maxDomain - minDomain + 1)
        medicinesPerDayChart.setDomainStep(StepMode.INCREMENT_BY_VAL, getDomainStepVal(numDomains))
        medicinesPerDayChart.setDomainBoundaries(minDomain, maxDomain, BoundaryMode.FIXED)

        medicinesPerDayChart.legend.setTableModel(
            DynamicTableModel(
                numLegendColumns,
                3,
                TableOrder.ROW_MAJOR
            )
        )

        setupRenderer(numDomains)
        medicinesPerDayChart.redraw()
    }

    private fun getFormatter(title: String): MedicinePerDayChartFormatter {
        val formatter = MedicinePerDayChartFormatter()
        formatter.fillPaint.setColor(getColor(title))
        formatter.borderPaint.setColor(context.getMaterialColor(androidx.appcompat.R.attr.colorPrimary))
        return formatter
    }

    private fun adjustTitleLength(series: SimpleXYSeries, columnPixels: Int, textPaint: Paint) {
        var titleWidth = textPaint.measureText(series.title).toInt()
        var title = series.title
        while (titleWidth > columnPixels && title.length > 5) {
            title = title.substring(0, title.length - 4) + "..."
            titleWidth = textPaint.measureText(title).toInt()
        }
        series.setTitle(title)
    }

    private fun calculateMaxRange(series: MutableList<SimpleXYSeries>): Number {
        if (series.isEmpty()) return 0
        return (0 until series[0].size()).maxOfOrNull { x ->
            series.sumOf { it.getY(x).toLong() }
        } ?: 0
    }

    private fun calculateMinDomain(series: MutableList<SimpleXYSeries>): Long {
        return if (series.isEmpty()) {
            LocalDate.now().toEpochDay() - 1
        } else {
            series[0].getX(0).toLong()
        }
    }

    private fun calculateMaxDomain(series: MutableList<SimpleXYSeries>): Long {
        return if (series.isEmpty()) {
            LocalDate.now().toEpochDay()
        } else {
            val first: XYSeries = series[0]
            first.getX(first.size() - 1).toLong()
        }
    }

    private fun getDomainStepVal(numDomains: Long): Double {
        return if (daysSinceEpochToDateString(
                context, LocalDate.of(2024, 12, 31).toEpochDay()
            ).length > 8
        ) {
            ceil((numDomains / 5.0f).toDouble())
        } else {
            ceil((numDomains / 7.0f).toDouble())
        }
    }

    private fun setupRenderer(numDomains: Long) {
        // The space between each bar and next to the outer bars is half bar width.
        // So we have numDomainsBar bars, numDomains - 1 spaces + 2 outer bars
        val numBars = numDomains + (numDomains - 1 + 2) / 2.0f
        val barWidth = medicinesPerDayChart.graph.widgetDimensions.paddedRect.width() / numBars

        medicinesPerDayChart.graph.setGridInsets(Insets(0f, 0f, barWidth, barWidth))

        val renderer = medicinesPerDayChart.getRenderer(
            MedicinePerDayChartRenderer::class.java
        )
        if (renderer != null) {
            renderer.setBarGroupWidth(BarRenderer.BarGroupWidthMode.FIXED_WIDTH, barWidth)
            renderer.barOrientation = BarRenderer.BarOrientation.STACKED
        }
    }

    private fun getColor(title: String): Int {
        val color =
            medicines.firstOrNull { it.medicine.name == title && it.medicine.useColor }?.medicine?.color
        return color ?: COLORS[colorIndex++ % COLORS.size]
    }

    private inner class DaysSinceEpochFormat : NumberFormat() {
        override fun format(
            value: Double, buffer: StringBuffer, field: FieldPosition
        ): StringBuffer {
            return buffer.append(daysSinceEpochToDateString(context, value.toLong()))
        }

        override fun format(
            value: Long, buffer: StringBuffer, field: FieldPosition
        ): StringBuffer {
            throw UnsupportedOperationException("Not yet implemented.")
        }

        override fun parse(string: String, position: ParsePosition): Number? {
            throw UnsupportedOperationException("Not yet implemented.")
        }
    }

    internal inner class MedicinePerDayChartFormatter : BarFormatter() {
        override fun getRendererClass(): Class<out BarRenderer<*>?> {
            return MedicinePerDayChartRenderer::class.java
        }

        override fun doGetRendererInstance(plot: XYPlot?): BarRenderer<*> {
            return MedicinePerDayChartRenderer(plot)
        }
    }

    internal inner class MedicinePerDayChartRenderer(plot: XYPlot?) :
        BarRenderer<MedicinePerDayChartFormatter?>(plot)
}
