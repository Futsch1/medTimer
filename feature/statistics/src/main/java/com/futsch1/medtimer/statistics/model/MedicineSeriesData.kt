package com.futsch1.medtimer.statistics.model

import androidx.compose.ui.graphics.Color

data class MedicineSeriesData(
    val name: String,
    val values: List<Int>,
    val color: Color,
)
