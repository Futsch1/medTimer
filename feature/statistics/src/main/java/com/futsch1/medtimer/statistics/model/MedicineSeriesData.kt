package com.futsch1.medtimer.statistics.model

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList

data class MedicineSeriesData(
    val name: String,
    val values: ImmutableList<Int>,
    val color: Color,
)
