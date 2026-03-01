package com.futsch1.medtimer.statistics.domain

import androidx.annotation.StringRes
import com.futsch1.medtimer.statistics.R

enum class AnalysisDays(val days: Int, @param:StringRes @field:StringRes val labelRes: Int) {
    ONE_DAY(1, R.string.twenty_four_hours),
    TWO_DAYS(2, R.string.two_days),
    THREE_DAYS(3, R.string.three_days),
    SEVEN_DAYS(7, R.string.seven_days),
    FOURTEEN_DAYS(14, R.string.fourteen_days),
    THIRTY_DAYS(30, R.string.thirty_days);

    companion object {
        val DEFAULT = SEVEN_DAYS

        fun fromDays(days: Int): AnalysisDays =
            entries.firstOrNull { it.days == days } ?: DEFAULT
    }
}