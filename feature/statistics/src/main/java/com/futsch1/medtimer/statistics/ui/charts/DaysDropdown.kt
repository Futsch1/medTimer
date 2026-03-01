package com.futsch1.medtimer.statistics.ui.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.statistics.domain.AnalysisDays
import com.futsch1.medtimer.statistics.ui.StatisticsTestTags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaysDropdown(
    selected: AnalysisDays,
    onSelected: (AnalysisDays) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "dropdownArrowRotation",
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        FilterChip(
            selected = true,
            onClick = {},
            label = { Text(stringResource(selected.labelRes)) },
            trailingIcon = {
                Icon(
                    Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                )
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .testTag(StatisticsTestTags.DAYS_DROPDOWN),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AnalysisDays.entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(stringResource(entry.labelRes)) },
                    onClick = {
                        onSelected(entry)
                        expanded = false
                    },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DaysDropdownPreview() {
    MedTimerTheme {
        Surface {
            DaysDropdown(
                selected = AnalysisDays.SEVEN_DAYS,
                onSelected = {},
            )
        }
    }
}