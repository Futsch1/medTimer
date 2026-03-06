package com.futsch1.medtimer.statistics.model

import kotlin.math.roundToInt

data class TakenSkippedData(
    val taken: Int,
    val skipped: Int,
    val title: String
) {
    val isEmpty: Boolean get() = taken + skipped == 0

    val takenPercent: Int
        get() = if (isEmpty) 0 else (100f * taken / (taken + skipped)).roundToInt()

    val skippedPercent: Int
        get() = if (isEmpty) 0 else 100 - takenPercent
}
