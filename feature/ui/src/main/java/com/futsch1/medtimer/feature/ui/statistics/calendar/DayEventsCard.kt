package com.futsch1.medtimer.feature.ui.statistics.calendar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
                Text(text = stringResource(R.string.no_reminders), style = MaterialTheme.typography.bodySmall)
                return@Column
            }
            val skippedLabel = stringResource(R.string.skipped)
            val raisedLabel = stringResource(R.string.raised)
            val takenLabel = stringResource(R.string.taken)
            val bodySmall = MaterialTheme.typography.bodySmall
            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val formattedTimes = remember(events, date) {
                events.map { event ->
                    val formatter = if (event.time.toLocalDate() == date) timeFormatter else dateTimeFormatter
                    event.time.format(formatter)
                }
            }
            val maxTimeWidth = remember(formattedTimes, bodySmall) {
                with(density) {
                    formattedTimes.maxOfOrNull { time ->
                        textMeasurer.measure(time, bodySmall).size.width
                    }?.toDp() ?: 0.dp
                }
            }
            events.forEachIndexed { index, event ->
                val statusLabel = when (event.status) {
                    CalendarDayEvent.Status.TAKEN -> takenLabel
                    CalendarDayEvent.Status.SKIPPED -> skippedLabel
                    CalendarDayEvent.Status.RAISED -> raisedLabel
                    CalendarDayEvent.Status.SCHEDULED -> null
                }
                val suffix = when (event.status) {
                    CalendarDayEvent.Status.SKIPPED -> " ($skippedLabel)"
                    CalendarDayEvent.Status.RAISED -> " ($raisedLabel)"
                    else -> ""
                }
                // A divider before every row, including the first, separates the date title from the
                // events and the events from each other — matching the legacy calendar's per-row divider.
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                EventRow(
                    time = formattedTimes[index],
                    timeWidth = maxTimeWidth,
                    statusIcon = statusIconRes(event.status),
                    statusLabel = statusLabel,
                    typeIcon = reminderTypeIconRes(event.reminderType),
                    amount = event.amount,
                    medicineName = event.medicineName,
                    suffix = suffix,
                    textStyle = bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EventRow(
    time: String,
    timeWidth: Dp,
    @DrawableRes statusIcon: Int?,
    statusLabel: String?,
    @DrawableRes typeIcon: Int,
    amount: String,
    medicineName: String,
    suffix: String,
    textStyle: TextStyle,
) {
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val eventText = buildAnnotatedString {
        append(amount)
        append(" ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(medicineName) }
        append(suffix)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Fixed-width status slot. SCHEDULED (future) events have no status icon; the empty Box keeps the
        // time column aligned across rows, mirroring the empty-pie legend's reserved space.
        Box(modifier = Modifier.size(STATUS_ICON_SIZE)) {
            if (statusIcon != null) {
                Icon(
                    painter = painterResource(statusIcon),
                    contentDescription = statusLabel,
                    tint = iconTint,
                    modifier = Modifier.size(STATUS_ICON_SIZE),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = time, style = textStyle, textAlign = TextAlign.End, modifier = Modifier.width(timeWidth))
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(typeIcon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(TYPE_ICON_SIZE),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = eventText, style = textStyle)
    }
}

@DrawableRes
private fun statusIconRes(status: CalendarDayEvent.Status): Int? = when (status) {
    CalendarDayEvent.Status.TAKEN -> R.drawable.ic_check_circle
    CalendarDayEvent.Status.SKIPPED -> R.drawable.ic_cancel
    CalendarDayEvent.Status.RAISED -> R.drawable.ic_notifications
    CalendarDayEvent.Status.SCHEDULED -> null
}

@DrawableRes
private fun reminderTypeIconRes(type: ReminderType): Int = when (type) {
    ReminderType.TIME_BASED -> R.drawable.ic_event
    ReminderType.LINKED -> R.drawable.ic_link
    ReminderType.CONTINUOUS_INTERVAL -> R.drawable.ic_repeat
    ReminderType.WINDOWED_INTERVAL -> R.drawable.ic_timelapse
    ReminderType.OUT_OF_STOCK -> R.drawable.ic_inventory_2
    ReminderType.EXPIRATION_DATE -> R.drawable.ic_event_busy
    ReminderType.REFILL -> R.drawable.ic_shopping_cart
}

private val STATUS_ICON_SIZE = 20.dp
private val TYPE_ICON_SIZE = 16.dp

@MedTimerPreview
@Composable
private fun DayEventsCardPreview() {
    val date = LocalDate.of(2026, 5, 28)
    MedTimerTheme {
        Surface {
            DayEventsCard(
                date = date,
                events = listOf(
                    CalendarDayEvent(date.atTime(8, 0), "1 tablet", "Vitamin X 500 mg", CalendarDayEvent.Status.TAKEN, ReminderType.TIME_BASED),
                    CalendarDayEvent(date.atTime(12, 30), "2 ml", "Medicine A", CalendarDayEvent.Status.SKIPPED, ReminderType.CONTINUOUS_INTERVAL),
                    CalendarDayEvent(date.atTime(18, 0), "1 dose", "Linked Med", CalendarDayEvent.Status.RAISED, ReminderType.LINKED),
                    CalendarDayEvent(date.atTime(20, 0), "1 capsule", "Supplement B", CalendarDayEvent.Status.SCHEDULED, ReminderType.WINDOWED_INTERVAL),
                ),
            )
        }
    }
}
