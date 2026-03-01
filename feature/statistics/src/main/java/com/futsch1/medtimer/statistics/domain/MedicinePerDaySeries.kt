package com.futsch1.medtimer.statistics.domain

data class MedicinePerDaySeries(
    val name: String, val xValues: List<Long>, val yValues: List<Int>
)