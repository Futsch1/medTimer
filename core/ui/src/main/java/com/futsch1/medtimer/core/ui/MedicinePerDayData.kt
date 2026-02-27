package com.futsch1.medtimer.core.ui

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

data class MedicinePerDayData(
    val title: String,
    val days: List<LocalDate>,
    val series: List<MedicineSeriesData>,
)

data class MedicineSeriesData(
    val name: String,
    val values: List<Int>,
    val color: Color?,
) {
    constructor(name: String, values: List<Int>, colorInt: Int) : this(
        name,
        values,
        Color(colorInt)
    )
}
