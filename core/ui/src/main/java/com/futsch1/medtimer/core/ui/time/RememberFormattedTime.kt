package com.futsch1.medtimer.core.ui.time

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Compose counterpart of TimeFormatter: the system's 12/24-hour clock, or a relative description
 * when [useRelativeTime]. A time on a different day than [sameDayAs] carries its date as well.
 */
@Composable
fun rememberFormattedTime(
    instant: Instant,
    useRelativeTime: Boolean = false,
    sameDayAs: Instant? = null,
): String {
    val context = LocalContext.current
    // Read through the configuration so a locale or clock-format change recomposes the caller.
    val configuration = LocalConfiguration.current
    return remember(instant, useRelativeTime, sameDayAs, configuration) {
        val zone = ZoneId.systemDefault()
        val withDate = sameDayAs != null && instant.atZone(zone).toLocalDate() != sameDayAs.atZone(zone).toLocalDate()
        when {
            useRelativeTime -> SystemDateTimeFormat.relativeDateTime(context, instant)
            withDate -> SystemDateTimeFormat.dateTime(context, instant)
            else -> SystemDateTimeFormat.time(context, instant)
        }
    }
}

/** A locale-aware short measure ("2 h 30 min"); the hour part is dropped when zero. */
fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val measures = buildList {
        if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
        add(Measure(minutes, MeasureUnit.MINUTE))
    }
    return MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.SHORT)
        .formatMeasures(*measures.toTypedArray())
}
