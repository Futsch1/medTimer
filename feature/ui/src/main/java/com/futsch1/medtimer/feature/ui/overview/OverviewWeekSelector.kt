package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    // simulatedThrough is LocalDate.MIN until the simulation repository first emits, which would
    // otherwise hand the calendar an end date before its start date.
    val effectiveRangeEnd = maxOf(rangeEnd, LocalDate.now().plusDays(DEFAULT_SIMULATION_DAYS))
    val state = rememberWeekCalendarState(
        startDate = rangeStart,
        endDate = effectiveRangeEnd,
        firstVisibleWeekDate = selectedDay,
        // Offsetting the week start by 3 days puts today at position 4 of 7 on first open.
        firstDayOfWeek = LocalDate.now().minusDays(3).dayOfWeek,
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

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {
                scope.launch { state.animateScrollToWeek(selectedDay.minusWeeks(1)) }
                onDaySelected(selectedDay.minusWeeks(1))
            },
            modifier = Modifier.testTag(OverviewTestTags.PREV_WEEK),
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
                scope.launch { state.animateScrollToWeek(selectedDay.plusWeeks(1)) }
                onDaySelected(selectedDay.plusWeeks(1))
            },
            modifier = Modifier.testTag(OverviewTestTags.NEXT_WEEK),
        ) {
            Icon(
                painter = painterResource(CoreUiR.drawable.chevron_right),
                contentDescription = stringResource(CoreUiR.string.next_week),
            )
        }
    }
}

@Composable
private fun WeekDayCell(day: WeekDay, isSelected: Boolean, onClick: () -> Unit) {
    val today = day.date == LocalDate.now()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
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
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                modifier = if (isSelected) Modifier.testTag(OverviewTestTags.SELECTED_DAY) else Modifier,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
