package com.futsch1.medtimer.statistics.ui.calendar

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import java.time.LocalDate

@Composable
fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit,
) {
    val isMonthDate = day.position == DayPosition.MonthDate

    val transition = updateTransition(
        targetState = isSelected,
        label = "dayCellSelection",
    )
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
            .sizeIn(maxWidth = 48.dp, maxHeight = 48.dp)
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (hasEvents) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
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

@PreviewLightDark
@Composable
private fun DayCellDefaultPreview() {
    MedTimerTheme {
        DayCell(
            day = CalendarDay(LocalDate.of(2024, 1, 15), DayPosition.MonthDate),
            isSelected = false,
            hasEvents = false,
            onClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DayCellWithEventsPreview() {
    MedTimerTheme {
        DayCell(
            day = CalendarDay(LocalDate.of(2024, 1, 15), DayPosition.MonthDate),
            isSelected = false,
            hasEvents = true,
            onClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DayCellSelectedPreview() {
    MedTimerTheme {
        DayCell(
            day = CalendarDay(LocalDate.of(2024, 1, 15), DayPosition.MonthDate),
            isSelected = true,
            hasEvents = true,
            onClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DayCellOutOfMonthPreview() {
    MedTimerTheme {
        DayCell(
            day = CalendarDay(LocalDate.of(2024, 1, 15), DayPosition.InDate),
            isSelected = false,
            hasEvents = false,
            onClick = {},
        )
    }
}
