package com.futsch1.medtimer.feature.ui.overview

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import com.kizitonwose.calendar.compose.weekcalendar.WeekCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.WeekDay
import kotlinx.coroutines.isActive
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
    val firstDayOfWeek = remember { LocalDate.now().minusDays(3).dayOfWeek }
    val state = rememberOverviewWeekCalendarState(selectedDay, rangeEnd, firstDayOfWeek)
    KeepSelectionInVisibleWeekEffect(state, selectedDay, onDaySelected)

    val scope = rememberCoroutineScope()
    fun selectAdjacentWeek(weeks: Long) {
        val target = adjacentWeekTarget(selectedDay, firstDayOfWeek, weeks, LocalDate.now())
        scope.launch { state.animateScrollToWeek(target) }
        onDaySelected(target)
    }

    Row(
        modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 400.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WeekArrow(CoreUiR.drawable.chevron_left, CoreUiR.string.previous_week) { selectAdjacentWeek(-1) }
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
        WeekArrow(CoreUiR.drawable.chevron_right, CoreUiR.string.next_week) { selectAdjacentWeek(1) }
    }
}

@Composable
private fun rememberOverviewWeekCalendarState(selectedDay: LocalDate, rangeEnd: LocalDate, firstDayOfWeek: DayOfWeek): WeekCalendarState {
    val rangeStart = remember { LocalDate.now().minusYears(3) }
    val effectiveRangeEnd = maxOf(rangeEnd, LocalDate.now().plusDays(DEFAULT_SIMULATION_DAYS))
    val state = rememberWeekCalendarState(
        startDate = rangeStart,
        endDate = effectiveRangeEnd,
        firstVisibleWeekDate = selectedDay,
        firstDayOfWeek = firstDayOfWeek,
    )

    // rememberWeekCalendarState only reads endDate on first composition.
    LaunchedEffect(effectiveRangeEnd) {
        state.endDate = effectiveRangeEnd
    }
    return state
}

/**
 * Scrolls the strip to [selectedDay], and moves the selection into the week the user scrolls to.
 *
 * The strip restores its saved week, which can predate the selection, so the visible week only moves the selection
 * once the scroll to the selection has settled or the user interrupted it. [WeekCalendar] reports no settled swipe,
 * so the parent is notified from the effect watching the visible week rather than from an event handler.
 */
@Composable
private fun KeepSelectionInVisibleWeekEffect(state: WeekCalendarState, selectedDay: LocalDate, onDaySelected: (LocalDate) -> Unit) {
    val currentSelectedDay by rememberUpdatedState(selectedDay)
    val currentOnDaySelected by rememberUpdatedState(onDaySelected)
    var scrollingToSelection by remember { mutableStateOf(true) }

    LaunchedEffect(selectedDay) {
        scrollingToSelection = true
        try {
            state.animateScrollToWeek(selectedDay)
        } finally {
            // Not when the next selection's scroll replaced this one; it owns the flag now.
            if (isActive) {
                scrollingToSelection = false
            }
        }
    }

    LaunchedEffect(state.firstVisibleWeek, scrollingToSelection) {
        if (scrollingToSelection) return@LaunchedEffect
        val dates = state.firstVisibleWeek.days.map { it.date }
        selectionForVisibleWeek(dates, currentSelectedDay, LocalDate.now())?.let(currentOnDaySelected)
    }
}

/** The day to select once [dates] are the visible week, or null when [selectedDay] is already among them. */
internal fun selectionForVisibleWeek(dates: List<LocalDate>, selectedDay: LocalDate, today: LocalDate): LocalDate? =
    when {
        selectedDay in dates -> null
        today in dates -> today
        dates.last() < selectedDay -> dates.last()
        else -> dates.first()
    }

/** The day an arrow selects [weeks] away from [selectedDay]'s week: today when it is in that week, otherwise its edge nearest to the selection. */
internal fun adjacentWeekTarget(selectedDay: LocalDate, firstDayOfWeek: DayOfWeek, weeks: Long, today: LocalDate): LocalDate {
    val targetWeekStart = weekStart(selectedDay, firstDayOfWeek).plusWeeks(weeks)
    val targetWeekEnd = targetWeekStart.plusDays(6)
    return today.takeIf { it in targetWeekStart..targetWeekEnd } ?: if (weeks < 0) targetWeekEnd else targetWeekStart
}

@Composable
private fun WeekArrow(@DrawableRes iconRes: Int, @StringRes descriptionRes: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(descriptionRes),
        )
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
