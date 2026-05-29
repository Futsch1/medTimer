package com.futsch1.medtimer.feature.ui.statistics

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/**
 * Per-day medicine intake counts for the Charts bar chart, decoupled from any chart library.
 *
 * [epochDays] are the x-axis days (ascending, one per day in the selected range); each
 * [MedicineDaySeries.counts] is aligned to [epochDays] (same size), stacked per medicine.
 */
data class MedicinePerDayData(
    val epochDays: List<Long>,
    val series: List<MedicineDaySeries>,
)

data class MedicineDaySeries(
    val medicineName: String,
    val counts: List<Int>,
)

/**
 * Rendered state for the Charts view: the per-day stacked bar data (+ x-axis labels and per-series
 * colors) and the taken/skipped counts for the two pies (selected range + all-time total).
 */
@Immutable
data class ChartsState(
    val perDay: MedicinePerDayData,
    val dayLabels: ImmutableList<String>,
    val seriesColors: ImmutableList<Int>,
    val takenPeriod: Long,
    val skippedPeriod: Long,
    val takenTotal: Long,
    val skippedTotal: Long,
    val days: Int,
)
