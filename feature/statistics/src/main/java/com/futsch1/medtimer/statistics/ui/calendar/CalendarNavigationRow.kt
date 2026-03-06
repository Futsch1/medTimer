package com.futsch1.medtimer.statistics.ui.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarNavigationRow(
    yearMonth: YearMonth,
    startMonth: YearMonth,
    endMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onYearSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev, enabled = yearMonth > startMonth) {
            Icon(Icons.AutoMirrored.Rounded.NavigateBefore, contentDescription = null)
        }

        YearMonthTitle(
            yearMonth = yearMonth,
            startYear = startMonth.year,
            endYear = endMonth.year,
            onYearSelected = onYearSelected,
        )

        IconButton(onClick = onNext, enabled = yearMonth < endMonth) {
            Icon(Icons.AutoMirrored.Rounded.NavigateNext, contentDescription = null)
        }
    }
}

@Composable
private fun YearMonthTitle(
    yearMonth: YearMonth,
    startYear: Int,
    endYear: Int,
    onYearSelected: (Int) -> Unit,
) {
    var showYearPicker by remember { mutableStateOf(false) }
    val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val rotation by animateFloatAsState(
        targetValue = if (showYearPicker) 180f else 0f,
        label = "yearPickerArrowRotation",
    )

    Box {
        Row(
            modifier = Modifier.clickable { showYearPicker = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$monthName ${yearMonth.year}",
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
            )
        }

        DropdownMenu(
            expanded = showYearPicker,
            onDismissRequest = { showYearPicker = false },
        ) {
            (startYear..endYear).forEach { year ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = year.toString(),
                            fontWeight = if (year == yearMonth.year) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        showYearPicker = false
                        onYearSelected(year)
                    },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun CalendarNavigationRowPreview() {
    MedTimerTheme {
        Surface {
            CalendarNavigationRow(
                yearMonth = YearMonth.of(2024, 6),
                startMonth = YearMonth.of(2024, 1),
                endMonth = YearMonth.of(2024, 12),
                onPrev = {},
                onNext = {},
                onYearSelected = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CalendarNavigationRowAtStartPreview() {
    MedTimerTheme {
        Surface {
            CalendarNavigationRow(
                yearMonth = YearMonth.of(2024, 1),
                startMonth = YearMonth.of(2024, 1),
                endMonth = YearMonth.of(2024, 12),
                onPrev = {},
                onNext = {},
                onYearSelected = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CalendarNavigationRowAtEndPreview() {
    MedTimerTheme {
        Surface {
            CalendarNavigationRow(
                yearMonth = YearMonth.of(2024, 12),
                startMonth = YearMonth.of(2024, 1),
                endMonth = YearMonth.of(2024, 12),
                onPrev = {},
                onNext = {},
                onYearSelected = {},
            )
        }
    }
}