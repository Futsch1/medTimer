package com.futsch1.medtimer.statistics.model

import java.time.LocalDate

data class MedicinePerDayData(
    val title: String,
    val days: List<LocalDate>,
    val series: List<MedicineSeriesData>,
)

