package com.futsch1.medtimer.feature.ui.statistics.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import java.time.YearMonth
import java.time.format.TextStyle

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
            Icon(painterResource(R.drawable.ic_navigate_before), contentDescription = null)
        }

        YearMonthTitle(
            yearMonth = yearMonth,
            startYear = startMonth.year,
            endYear = endMonth.year,
            onYearSelected = onYearSelected,
        )

        IconButton(onClick = onNext, enabled = yearMonth < endMonth) {
            Icon(painterResource(R.drawable.ic_navigate_next), contentDescription = null)
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
    val locale = LocalConfiguration.current.locales[0]
    val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, locale)
    val rotation by animateFloatAsState(
        targetValue = if (showYearPicker) 180f else 0f,
        label = "yearPickerArrowRotation",
    )

    Box {
        Row(
            modifier = Modifier.clickable { showYearPicker = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "$monthName ${yearMonth.year}", style = MaterialTheme.typography.titleMedium)
            Icon(
                painter = painterResource(R.drawable.ic_arrow_drop_down),
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
            )
        }

        DropdownMenu(expanded = showYearPicker, onDismissRequest = { showYearPicker = false }) {
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

@MedTimerPreview
@Composable
private fun CalendarNavigationRowPreview() {
    val month = YearMonth.of(2026, 5)
    MedTimerTheme {
        Surface {
            CalendarNavigationRow(
                yearMonth = month,
                startMonth = month.minusMonths(3),
                endMonth = month,
                onPrev = {},
                onNext = {},
                onYearSelected = {},
            )
        }
    }
}
