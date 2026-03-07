package com.futsch1.medtimer.statistics

import android.content.Context
import com.androidplot.pie.PieChart
import com.androidplot.pie.PieRenderer
import com.androidplot.pie.Segment
import com.androidplot.pie.SegmentFormatter
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.dpToPx
import com.futsch1.medtimer.helpers.getMaterialColor
import java.util.Locale
import kotlin.math.roundToInt

class TakenSkippedChart(private val pieChart: PieChart, private val context: Context) {
    private val segmentTaken: Segment = Segment(context.getString(R.string.taken), 0)
    private val segmentSkipped: Segment = Segment(context.getString(R.string.skipped), 0)

    init {
        pieChart.addSegment(
            segmentTaken,
            getFormatter(
                androidx.appcompat.R.attr.colorPrimary,
                com.google.android.material.R.attr.colorOnPrimary
            )
        )
        pieChart.addSegment(
            segmentSkipped,
            getFormatter(
                com.google.android.material.R.attr.colorSecondary,
                com.google.android.material.R.attr.colorOnSecondary
            )
        )
        pieChart.plotPaddingTop = context.resources.dpToPx(5.0f)
        pieChart.backgroundPaint
            .setColor(context.getMaterialColor(com.google.android.material.R.attr.colorSurface))
        pieChart.title.labelPaint
            .setColor(context.getMaterialColor(com.google.android.material.R.attr.colorOnSurface))

        val renderer = pieChart.getRenderer(PieRenderer::class.java)
        renderer.setDonutSize(0.0f, PieRenderer.DonutMode.PERCENT)

    }

    fun getFormatter(colorSegment: Int, colorText: Int): SegmentFormatter {
        val formatter = SegmentFormatter(context.getMaterialColor(colorSegment))
        formatter.labelPaint.setColor(context.getMaterialColor(colorText))
        return formatter
    }

    fun updateData(taken: Long, skipped: Long, days: Int) {
        val title = if (days != 0) {
            context.resources.getQuantityString(R.plurals.last_n_days, days, days)
        } else {
            context.getString(R.string.total)
        }
        pieChart.setTitle(title)

        val totalValue = taken + skipped
        setupSegment(segmentTaken, taken, totalValue, R.string.taken)
        setupSegment(segmentSkipped, skipped, totalValue, R.string.skipped)
        pieChart.redraw()
    }

    private fun setupSegment(segment: Segment, value: Long, totalValue: Long, stringId: Int) {
        segment.value = value
        if (value <= 0) {
            segment.title = ""
            return
        }

        val stringValue = context.getString(stringId)
        val percentageValue = (100 * value.toFloat() / totalValue).roundToInt()

        segment.title = "$stringValue: ${"%d%%".format(Locale.US, percentageValue)}"
    }
}
