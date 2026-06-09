package com.futsch1.medtimer.feature.ui.statistics.charts

import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.ui.statistics.ChartsData
import com.futsch1.medtimer.feature.ui.statistics.ChartsState
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

/**
 * Turns aggregated [ChartsData] into the render-ready [ChartsState]: formats per-day labels, assigns
 * series colors, and copies the taken/skipped tallies through. [medicineColorsByName] holds the
 * custom color (ARGB int) of every medicine that opted in; everything else falls back to the palette.
 */
class ChartsPresenter @Inject constructor(
    private val timeFormatter: TimeFormatter,
) {
    fun present(data: ChartsData, medicineColorsByName: Map<String, Int>, days: Int): ChartsState {
        val seriesColors = ChartSeriesColors.assign(data.perDay.series.map { it.medicineName }, medicineColorsByName)
        return ChartsState(
            perDay = data.perDay,
            dayLabels = data.perDay.epochDays.map { timeFormatter.daysSinceEpochToDateString(it) }.toImmutableList(),
            seriesColors = seriesColors.toImmutableList(),
            takenPeriod = data.period.taken,
            skippedPeriod = data.period.skipped,
            takenTotal = data.total.taken,
            skippedTotal = data.total.skipped,
            days = days,
        )
    }
}
