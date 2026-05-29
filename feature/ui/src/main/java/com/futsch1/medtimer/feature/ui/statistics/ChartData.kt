package com.futsch1.medtimer.feature.ui.statistics

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

// Decoupled from Vico: ViewModel builds this; composables map it to chart-library types.
data class MedicinePerDayData(
    val epochDays: List<Long>,
    val series: List<MedicineDaySeries>,
)

data class MedicineDaySeries(
    val medicineName: String,
    val counts: List<Int>,
)

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
