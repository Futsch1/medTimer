package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.time.currentLocale
import com.futsch1.medtimer.feature.reminders.api.SimulatedReminders.Companion.DEFAULT_SIMULATION_DAYS
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.WeekDay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import com.futsch1.medtimer.core.ui.R as CoreUiR

/**
 * Week strip with previous/next arrows. The week shown follows [selectedDay], and scrolling to a week
 * that does not contain the selection moves the selection into it — today when it is in range,
 * otherwise the nearest edge, so the selection is always visible.
 */
@Composable
fun OverviewWeekSelector(
    selectedDay: LocalDate,
    rangeEnd: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rangeStart = remember { LocalDate.now().minusYears(3) }
    val effectiveRangeEnd = maxOf(rangeEnd, LocalDate.now().plusDays(DEFAULT_SIMULATION_DAYS))
    val firstDayOfWeek = remember { LocalDate.now().minusDays(3).dayOfWeek }
    val state = rememberWeekCalendarState(
        startDate = rangeStart,
        endDate = effectiveRangeEnd,
        firstVisibleWeekDate = selectedDay,
        firstDayOfWeek = firstDayOfWeek,
    )
    val scope = rememberCoroutineScope()

    // rememberWeekCalendarState only reads endDate on first composition.
    LaunchedEffect(effectiveRangeEnd) {
        state.endDate = effectiveRangeEnd
    }

    LaunchedEffect(selectedDay) {
        state.animateScrollToWeek(selectedDay)
    }

    LaunchedEffect(state.firstVisibleWeek) {
        val dates = state.firstVisibleWeek.days.map { it.date }
        if (selectedDay in dates) return@LaunchedEffect
        val today = LocalDate.now()
        onDaySelected(
            when {
                today in dates -> today
                dates.last() < selectedDay -> dates.last()
                else -> dates.first()
            }
        )
    }

    Row(
        modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 400.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                val previousWeekStart = weekStart(selectedDay, firstDayOfWeek).minusWeeks(1)
                val previousWeekEnd = previousWeekStart.plusDays(6)
                val target = LocalDate.now().takeIf { it in previousWeekStart..previousWeekEnd } ?: previousWeekEnd
                scope.launch { state.animateScrollToWeek(target) }
                onDaySelected(target)
            },
        ) {
            Icon(
                painter = painterResource(CoreUiR.drawable.chevron_left),
                contentDescription = stringResource(CoreUiR.string.previous_week),
            )
        }
        WeekCalendar(
            state = state,
            modifier = Modifier.weight(1f),
            dayContent = { day ->
                WeekDayCell(
                    day = day,
                    isSelected = day.date == selectedDay,
                    onClick = { onDaySelected(day.date) },
                )
            },
        )
        IconButton(
            onClick = {
                val nextWeekStart = weekStart(selectedDay, firstDayOfWeek).plusWeeks(1)
                val nextWeekEnd = nextWeekStart.plusDays(6)
                val target = LocalDate.now().takeIf { it in nextWeekStart..nextWeekEnd } ?: nextWeekStart
                scope.launch { state.animateScrollToWeek(target) }
                onDaySelected(target)
            },
        ) {
            Icon(
                painter = painterResource(CoreUiR.drawable.chevron_right),
                contentDescription = stringResource(CoreUiR.string.next_week),
            )
        }
    }
}

/** The first date of the week containing [date], per [firstDayOfWeek]. */
private fun weekStart(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate {
    val daysFromWeekStart = (date.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    return date.minusDays(daysFromWeekStart.toLong())
}

@Composable
private fun WeekDayCell(day: WeekDay, isSelected: Boolean, onClick: () -> Unit) {
    val today = day.date == LocalDate.now()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Text(
            text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, currentLocale),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .testTag(OverviewTestTags.day(day.date))
                .selectable(selected = isSelected, onClick = onClick)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
