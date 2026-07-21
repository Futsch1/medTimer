package com.futsch1.medtimer.feature.ui.impl.statistics.calendar

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.getIcon
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun DayEventsCard(
    date: LocalDate,
    events: List<CalendarDayEvent>,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val dateTimeFormatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = date.format(dateFormatter),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            if (events.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_reminders),
                    style = MaterialTheme.typography.bodySmall
                )
                return@Column
            }
            val skippedLabel = stringResource(R.string.skipped)
            val raisedLabel = stringResource(R.string.raised)
            val takenLabel = stringResource(R.string.taken)
            val bodySmall = MaterialTheme.typography.bodySmall
            // Reminded and taken times are formatted here so the structured model stays free of strings;
            // a same-day timestamp shows the time only, otherwise the full date and time.
            val formattedTimes = remember(events, date) {
                events.map { event ->
                    fun format(time: LocalDateTime) =
                        time.format(if (time.toLocalDate() == date) timeFormatter else dateTimeFormatter)
                    format(event.time) to event.takenTime?.let(::format)
                }
            }
            events.forEachIndexed { index, event ->
                val statusLabel = when (event.status) {
                    CalendarDayEvent.Status.TAKEN -> takenLabel
                    CalendarDayEvent.Status.SKIPPED -> skippedLabel
                    CalendarDayEvent.Status.RAISED -> raisedLabel
                    CalendarDayEvent.Status.SCHEDULED -> null
                }
                val (remindedTime, takenTime) = formattedTimes[index]
                val intervalText = event.interval?.let {
                    "(${stringResource(
                        R.string.interval_time,
                        formatInterval(it)
                    )})"
                }
                // A divider before every row, including the first, separates the date title from the
                // events and the events from each other — matching the legacy calendar's per-row divider.
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                EventRow(
                    time = remindedTime,
                    takenTime = takenTime,
                    intervalText = intervalText,
                    statusIcon = statusIconRes(event.status),
                    statusLabel = statusLabel,
                    typeIcon = event.reminderType.getIcon(),
                    amount = event.amount,
                    medicineName = event.medicineName,
                    textStyle = bodySmall,
                )
            }
        }
    }
}

@Composable
@Suppress("kotlin:S107")
private fun EventRow(
    time: String,
    takenTime: String?,
    intervalText: String?,
    @DrawableRes statusIcon: Int?,
    statusLabel: String?,
    @DrawableRes typeIcon: Int,
    amount: String,
    medicineName: String,
    textStyle: TextStyle,
) {
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (statusIcon != null) {
                Icon(
                    painter = painterResource(statusIcon),
                    contentDescription = statusLabel,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Icon(
                painter = painterResource(typeIcon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp),
            )
            Text(text = time, style = textStyle)
            // The taken time follows the reminded time after an arrow, e.g. "8:00 → 8:42".
            if (takenTime != null) {
                Icon(
                    painter = painterResource(R.drawable.arrow_right),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
                Text(text = takenTime, style = textStyle)
            }
            if (intervalText != null) {
                Text(text = intervalText, style = textStyle)
            }
        }
        // Medicine name on its own row; the status suffix stays unstyled next to the bold name.
        Row {
            Text(text = medicineName, style = textStyle, fontWeight = FontWeight.Bold)
            if (amount.isNotEmpty()) {
                Text(text = " ($amount)", style = textStyle)
            }
        }
    }
}

// The reminder-type icon comes from the shared ReminderType.getIcon() mapping in :core:ui so the
// calendar matches the rest of the app; only the status icons are calendar-local.
@DrawableRes
private fun statusIconRes(status: CalendarDayEvent.Status): Int? = when (status) {
    CalendarDayEvent.Status.TAKEN -> R.drawable.check2_circle
    CalendarDayEvent.Status.SKIPPED -> R.drawable.x_circle
    CalendarDayEvent.Status.RAISED -> R.drawable.bell
    CalendarDayEvent.Status.SCHEDULED -> null
}

// Formats an interval as a locale-aware short measure ("2 h 30 min"), mirroring the legacy overview
// formatter: the hour part is dropped when zero, the minute part is always shown.
private fun formatInterval(interval: Duration): String {
    val totalSeconds = interval.seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val measures = buildList {
        if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
        add(Measure(minutes, MeasureUnit.MINUTE))
    }
    return MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.SHORT)
        .formatMeasures(*measures.toTypedArray())
}

@MedTimerPreview
@Composable
private fun DayEventsCardPreview() {
    val date = LocalDate.of(2026, 5, 28)
    MedTimerTheme {
        Surface {
            DayEventsCard(
                date = date,
                events = listOf(
                    CalendarDayEvent(
                        date.atTime(8, 0),
                        "1 tablet",
                        "Vitamin X 500 mg",
                        CalendarDayEvent.Status.TAKEN,
                        ReminderType.CONTINUOUS_INTERVAL,
                        takenTime = date.atTime(8, 42),
                        interval = Duration.ofMinutes(150),
                    ),
                    CalendarDayEvent(
                        date.atTime(12, 30),
                        "2 ml",
                        "Medicine A",
                        CalendarDayEvent.Status.SKIPPED,
                        ReminderType.CONTINUOUS_INTERVAL
                    ),
                    CalendarDayEvent(
                        date.atTime(18, 0),
                        "1 dose",
                        "Linked Med",
                        CalendarDayEvent.Status.RAISED,
                        ReminderType.LINKED
                    ),
                    CalendarDayEvent(
                        date.atTime(20, 0),
                        "1 capsule",
                        "Supplement B",
                        CalendarDayEvent.Status.SCHEDULED,
                        ReminderType.WINDOWED_INTERVAL
                    ),
                ),
            )
        }
    }
}
