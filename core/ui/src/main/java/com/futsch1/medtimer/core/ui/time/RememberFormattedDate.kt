package com.futsch1.medtimer.core.ui.time

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale

/** The active locale, read through the configuration so a locale change recomposes the caller. */
val currentLocale: Locale
    @Composable @ReadOnlyComposable get() = LocalConfiguration.current.locales[0]

/** The system's short-date format. */
@Composable
fun rememberFormattedDate(date: LocalDate): String {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(date, configuration) { SystemDateTimeFormat.date(context, date) }
}

/**
 * Formats [temporal] using the locale's preferred pattern for [skeleton] — an ICU skeleton such as
 * "MMMMy". A fixed pattern would translate the field names but keep the English field order, which
 * reads wrong in locales that write the year first.
 */
@Composable
fun rememberFormattedDate(temporal: TemporalAccessor, skeleton: String): String {
    val locale = currentLocale
    return remember(temporal, skeleton, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
        DateTimeFormatter.ofPattern(pattern, locale).format(temporal)
    }
}
