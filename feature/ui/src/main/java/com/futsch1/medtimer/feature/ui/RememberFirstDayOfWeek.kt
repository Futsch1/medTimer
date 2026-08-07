package com.futsch1.medtimer.feature.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.futsch1.medtimer.core.ui.time.currentLocale
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.DayOfWeek

/** The active locale's first day of week, recomposing when [currentLocale] changes. */
@Composable
fun rememberFirstDayOfWeek(): DayOfWeek {
    val locale = currentLocale
    return remember(locale) { firstDayOfWeekFromLocale(locale) }
}
