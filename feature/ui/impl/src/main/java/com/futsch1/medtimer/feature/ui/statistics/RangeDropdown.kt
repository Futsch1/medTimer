package com.futsch1.medtimer.feature.ui.statistics

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme

/** Analysis ranges shown in [RangeDropdown], in display order: label string resource -> number of days. */
val ANALYSIS_RANGES: List<Pair<Int, Int>> = listOf(
    R.string.twenty_four_hours to 1,
    R.string.two_days to 2,
    R.string.three_days to 3,
    R.string.seven_days to 7,
    R.string.fourteen_days to 14,
    R.string.thirty_days to 30,
)

/** Chip-styled dropdown for picking the Analysis time range (drives the Charts view only). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RangeDropdown(days: Int, onSelectRange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = (ANALYSIS_RANGES.firstOrNull { it.second == days } ?: ANALYSIS_RANGES.first()).first
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rangeArrowRotation")

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        FilterChip(
            selected = true,
            onClick = {},
            label = { Text(stringResource(selectedLabel)) },
            trailingIcon = {
                Icon(painterResource(R.drawable.caret_down_fill), contentDescription = null, modifier = Modifier.rotate(rotation))
            },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ANALYSIS_RANGES.forEach { (labelRes, value) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        expanded = false
                        onSelectRange(value)
                    },
                )
            }
        }
    }
}

@MedTimerPreview
@Composable
private fun RangeDropdownPreview() {
    MedTimerTheme {
        Surface {
            RangeDropdown(days = 7, onSelectRange = {})
        }
    }
}
