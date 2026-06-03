package com.futsch1.medtimer.feature.ui.statistics.calendar

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import java.time.LocalDate

@Composable
fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit,
    // Upper bound on a (square) cell. Portrait keeps 48dp; tablet landscape passes a larger computed
    // size so the grid scales up to fill its card while staying square.
    maxCellSize: Dp = 48.dp,
) {
    val isMonthDate = day.position == DayPosition.MonthDate

    val transition = updateTransition(targetState = isSelected, label = "dayCellSelection")
    val backgroundColor by transition.animateColor(
        label = "dayCellBackground",
        transitionSpec = { tween(durationMillis = 300) },
    ) { selected ->
        val result = when {
            selected -> MaterialTheme.colorScheme.secondary
            hasEvents -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surface
        }
        if (isMonthDate) result else result.copy(alpha = 0.5f)
    }
    val textColor by transition.animateColor(
        label = "dayCellText",
        transitionSpec = { tween(durationMillis = 300) },
    ) { selected ->
        val result = when {
            selected -> MaterialTheme.colorScheme.onSecondary
            hasEvents -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        }
        if (isMonthDate) result else result.copy(alpha = 0.5f)
    }
    val fontWeight = if (hasEvents) FontWeight.Bold else FontWeight.Normal

    Box(
        modifier = Modifier
            .sizeIn(maxWidth = maxCellSize, maxHeight = maxCellSize)
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = fontWeight),
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

@MedTimerPreview
@Composable
private fun DayCellPreview() {
    val date = LocalDate.of(2026, 5, 28)
    MedTimerTheme {
        Surface {
            // Plain, has-events, selected, and an out-of-month (dimmed) day.
            Row {
                DayCell(CalendarDay(date, DayPosition.MonthDate), isSelected = false, hasEvents = false, onClick = {})
                DayCell(CalendarDay(date, DayPosition.MonthDate), isSelected = false, hasEvents = true, onClick = {})
                DayCell(CalendarDay(date, DayPosition.MonthDate), isSelected = true, hasEvents = true, onClick = {})
                DayCell(CalendarDay(date, DayPosition.OutDate), isSelected = false, hasEvents = false, onClick = {})
            }
        }
    }
}
