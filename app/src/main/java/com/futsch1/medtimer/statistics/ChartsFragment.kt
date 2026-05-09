package com.futsch1.medtimer.statistics

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.androidplot.pie.PieChart
import com.androidplot.pie.PieRenderer
import com.androidplot.pie.Segment
import com.androidplot.pie.SegmentFormatter
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
import com.futsch1.medtimer.R
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.helpers.dpToPx
import com.futsch1.medtimer.helpers.getMaterialColor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@AndroidEntryPoint
class ChartsFragment : Fragment() {
    private val viewModel: ChartsViewModel by viewModels({ requireParentFragment() })

    @Inject
    @Dispatcher(MedTimerDispatchers.Main)
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    @Dispatcher(MedTimerDispatchers.Default)
    lateinit var backgroundDispatcher: CoroutineDispatcher

    @Inject
    lateinit var timeFormatter: TimeFormatter

    private lateinit var takenSkippedChartView: PieChart
    private lateinit var takenSkippedTotalChartView: PieChart
    private lateinit var medicinesPerDayChartView: XYPlot

    private lateinit var segmentTakenPeriod: Segment
    private lateinit var segmentSkippedPeriod: Segment
    private lateinit var segmentTakenTotal: Segment
    private lateinit var segmentSkippedTotal: Segment

    private var medicinesPerDayChartInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val statisticsView = inflater.inflate(R.layout.fragment_charts, container, false)

        medicinesPerDayChartView = statisticsView.findViewById(R.id.medicinesPerDayChart)
        takenSkippedChartView = statisticsView.findViewById(R.id.takenSkippedChart)
        takenSkippedTotalChartView = statisticsView.findViewById(R.id.takenSkippedChartTotal)

        setupTakenSkippedCharts()
        setupMedicinesPerDayChart()
        viewLifecycleOwner.lifecycleScope.launch(backgroundDispatcher) {
            viewModel.uiState.filterNotNull().collect { state ->
                withContext(mainDispatcher) {
                    updateMedicinesPerDayChart(state)
                    updateTakenSkipped(
                        takenSkippedChartView,
                        segmentTakenPeriod,
                        segmentSkippedPeriod,
                        state.takenPeriod,
                        state.skippedPeriod,
                        state.days
                    )
                    updateTakenSkipped(
                        takenSkippedTotalChartView,
                        segmentTakenTotal,
                        segmentSkippedTotal,
                        state.takenTotal,
                        state.skippedTotal,
                        0
                    )
                }
            }
        }

        return statisticsView
    }

    private fun setupTakenSkippedCharts() {
        segmentTakenPeriod = Segment(requireContext().getString(R.string.taken), 0)
        segmentSkippedPeriod = Segment(requireContext().getString(R.string.skipped), 0)
        setupPieChart(takenSkippedChartView, segmentTakenPeriod, segmentSkippedPeriod)

        segmentTakenTotal = Segment(requireContext().getString(R.string.taken), 0)
        segmentSkippedTotal = Segment(requireContext().getString(R.string.skipped), 0)
        setupPieChart(takenSkippedTotalChartView, segmentTakenTotal, segmentSkippedTotal)
    }

    private fun setupPieChart(pieChart: PieChart, segmentTaken: Segment, segmentSkipped: Segment) {
        pieChart.addSegment(
            segmentTaken,
            getPieSegmentFormatter(
                androidx.appcompat.R.attr.colorPrimary,
                com.google.android.material.R.attr.colorOnPrimary
            )
        )
        pieChart.addSegment(
            segmentSkipped,
            getPieSegmentFormatter(
                com.google.android.material.R.attr.colorSecondary,
                com.google.android.material.R.attr.colorOnSecondary
            )
        )
        pieChart.plotPaddingTop = requireContext().resources.dpToPx(5.0f)
        pieChart.backgroundPaint.color = requireContext().getMaterialColor(
            com.google.android.material.R.attr.colorSurface,
            "TakenSkippedChart"
        )
        pieChart.title.labelPaint.color = requireContext().getMaterialColor(
            com.google.android.material.R.attr.colorOnSurface,
            "TakenSkippedChart"
        )
        val renderer = pieChart.getRenderer(PieRenderer::class.java)
        renderer.setDonutSize(0.0f, PieRenderer.DonutMode.PERCENT)
    }

    private fun getPieSegmentFormatter(colorSegment: Int, colorText: Int): SegmentFormatter {
        val formatter = SegmentFormatter(
            requireContext().getMaterialColor(colorSegment, "TakenSkippedChart")
        )
        formatter.labelPaint.color = requireContext().getMaterialColor(colorText, "TakenSkippedChart")
        return formatter
    }

    private fun updateTakenSkipped(
        pieChart: PieChart,
        segmentTaken: Segment,
        segmentSkipped: Segment,
        taken: Long,
        skipped: Long,
        days: Int
    ) {
        val title = if (days != 0) {
            requireContext().resources.getQuantityString(R.plurals.last_n_days, days, days)
        } else {
            requireContext().getString(R.string.total)
        }
        pieChart.setTitle(title)

        val totalValue = taken + skipped
        setupPieSegment(segmentTaken, taken, totalValue, R.string.taken)
        setupPieSegment(segmentSkipped, skipped, totalValue, R.string.skipped)
        pieChart.redraw()
    }

    private fun setupPieSegment(segment: Segment, value: Long, totalValue: Long, stringId: Int) {
        segment.value = value
        if (value <= 0) {
            segment.title = ""
            return
        }
        val stringValue = requireContext().getString(stringId)
        val percentageValue = (100 * value.toFloat() / totalValue).roundToInt()
        segment.title = "$stringValue: ${"%d%%".format(Locale.US, percentageValue)}"
    }

    private fun setupMedicinesPerDayChart() {
        try {
            medicinesPerDayChartView.setRangeLowerBoundary(0, BoundaryMode.FIXED)
            setupBottomLine()
            setupLeftLine()
            setupNoBackgrounds()
            setupLegend()
            medicinesPerDayChartInitialized = true
        } catch (_: IllegalStateException) {
            // Intentionally ignored
        }
    }

    private fun Paint.applyAxisLabelStyle() {
        val context = requireContext()
        textSize = context.resources.dpToPx(10.0f)
        color = context.getMaterialColor(com.google.android.material.R.attr.colorOnSurface, "TakenSkippedChart")
    }

    private fun setupBottomLine() {
        val bottomLine = medicinesPerDayChartView.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM)
        bottomLine.format = DaysSinceEpochFormat()
        bottomLine.paint.textAlign = Paint.Align.CENTER
        bottomLine.paint.applyAxisLabelStyle()
    }

    private fun setupLeftLine() {
        val leftLine = medicinesPerDayChartView.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT)
        leftLine.format = DecimalFormat("#")
        leftLine.paint.applyAxisLabelStyle()
    }

    private fun setupNoBackgrounds() {
        medicinesPerDayChartView.backgroundPaint = null
        medicinesPerDayChartView.graph.rangeGridLinePaint = null
        medicinesPerDayChartView.graph.domainGridLinePaint = null
        medicinesPerDayChartView.graph.backgroundPaint = null
        medicinesPerDayChartView.graph.gridBackgroundPaint = null
        medicinesPerDayChartView.graph.domainSubGridLinePaint = null
        medicinesPerDayChartView.graph.rangeSubGridLinePaint = null
        medicinesPerDayChartView.graph.domainOriginLinePaint = null
        medicinesPerDayChartView.graph.rangeOriginLinePaint = null
    }

    private fun setupLegend() {
        val legend = medicinesPerDayChartView.legend
        legend.textPaint.applyAxisLabelStyle()
        legend.setPadding(requireContext().resources.dpToPx(4.0f), 0f, requireContext().resources.dpToPx(4.0f), 0f)
    }

    private fun updateMedicinesPerDayChart(state: ChartsUiState) {
        if (!medicinesPerDayChartInitialized) return
        val series = state.series

        medicinesPerDayChartView.clear()
        val numLegendColumns = max(3, ceil((series.size / 3.0)).toInt())
        var columnSize = medicinesPerDayChartView.legend.widgetDimensions.paddedRect.width()
            .toInt() / numLegendColumns
        columnSize -= medicinesPerDayChartView.legend.iconSize.width.getPixelValue(0.0f).toInt() + 2

        series.forEachIndexed { index, xySeries ->
            medicinesPerDayChartView.addSeries(xySeries, getBarFormatter(state.seriesColors[index]))
            adjustTitleLength(xySeries, columnSize, medicinesPerDayChartView.legend.textPaint)
        }

        medicinesPerDayChartView.setRangeUpperBoundary(state.rangeMax, BoundaryMode.FIXED)
        medicinesPerDayChartView.setRangeStep(StepMode.INCREMENT_BY_VAL, 1.0)
        medicinesPerDayChartView.setDomainStep(StepMode.INCREMENT_BY_VAL, state.domainStep)
        medicinesPerDayChartView.setDomainBoundaries(state.domainMin, state.domainMax, BoundaryMode.FIXED)

        medicinesPerDayChartView.legend.setTableModel(
            DynamicTableModel(numLegendColumns, 3, TableOrder.ROW_MAJOR)
        )

        setupBarRenderer(state.domainMax - state.domainMin + 1)
        medicinesPerDayChartView.redraw()
    }

    private fun getBarFormatter(color: Int): MedicinePerDayChartFormatter {
        val formatter = MedicinePerDayChartFormatter()
        formatter.fillPaint.color = color
        formatter.borderPaint.color = requireContext().getMaterialColor(
            androidx.appcompat.R.attr.colorPrimary,
            "TakenSkippedChart"
        )
        return formatter
    }

    private fun adjustTitleLength(series: SimpleXYSeries, columnPixels: Int, textPaint: Paint) {
        var titleWidth = textPaint.measureText(series.title).toInt()
        var title = series.title
        while (titleWidth > columnPixels && title.length > 5) {
            title = title.dropLast(4) + "..."
            titleWidth = textPaint.measureText(title).toInt()
        }
        series.setTitle(title)
    }

    private fun setupBarRenderer(numDomains: Long) {
        // The space between each bar and next to the outer bars is half bar width.
        // So we have numDomainsBar bars, numDomains - 1 spaces + 2 outer bars
        val numBars = numDomains + (numDomains - 1 + 2) / 2.0f
        val barWidth = medicinesPerDayChartView.graph.widgetDimensions.paddedRect.width() / numBars

        medicinesPerDayChartView.graph.setGridInsets(Insets(0f, 0f, barWidth, barWidth))

        val renderer = medicinesPerDayChartView.getRenderer(MedicinePerDayChartRenderer::class.java)
        if (renderer != null) {
            renderer.setBarGroupWidth(BarRenderer.BarGroupWidthMode.FIXED_WIDTH, barWidth)
            renderer.barOrientation = BarRenderer.BarOrientation.STACKED
        }
    }

    private inner class DaysSinceEpochFormat : NumberFormat() {
        override fun format(
            value: Double, buffer: StringBuffer, field: FieldPosition
        ): StringBuffer {
            return buffer.append(timeFormatter.daysSinceEpochToDateString(value.toLong()))
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
