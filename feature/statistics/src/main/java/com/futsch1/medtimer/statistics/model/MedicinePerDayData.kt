package com.futsch1.medtimer.statistics.model

import kotlinx.collections.immutable.ImmutableList
import java.time.LocalDate

data class MedicinePerDayData(
    val title: String,
    val days: ImmutableList<LocalDate>,
    val series: ImmutableList<MedicineSeriesData>,
)

