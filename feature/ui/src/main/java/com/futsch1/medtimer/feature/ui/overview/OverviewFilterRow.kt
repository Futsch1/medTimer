package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.core.ui.R as CoreUiR

private val FILTERS = listOf(
    Triple(OverviewFilter.TAKEN, CoreUiR.drawable.check2_circle, CoreUiR.string.taken),
    Triple(OverviewFilter.SKIPPED, CoreUiR.drawable.x_circle, CoreUiR.string.skipped),
    Triple(OverviewFilter.RAISED, CoreUiR.drawable.bell, CoreUiR.string.raised),
    Triple(OverviewFilter.SCHEDULED, CoreUiR.drawable.alarm, CoreUiR.string.scheduled),
)

private val BUTTON_SIZE = 40.dp
private const val HIGHLIGHT_ANIMATION_MILLIS = 250

private val ROW_SHAPE = RoundedCornerShape(BUTTON_SIZE * 0.4f)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OverviewFilterRow(
    activeFilters: Set<OverviewFilter>,
    onToggleFilter: (OverviewFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonShape = MaterialShapes.Square.toShape()

    Row(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ROW_SHAPE)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            FILTERS.forEach { (filter, iconRes, labelRes) ->
                val label = stringResource(labelRes)
                val checked = filter in activeFilters
                val containerColor by animateColorAsState(
                    targetValue = if (checked) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    animationSpec = tween(HIGHLIGHT_ANIMATION_MILLIS),
                    label = "filter_container",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (checked) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(HIGHLIGHT_ANIMATION_MILLIS),
                    label = "filter_content",
                )

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text(label) } },
                    state = rememberTooltipState(),
                ) {
                    IconToggleButton(
                        checked = checked,
                        onCheckedChange = { onToggleFilter(filter) },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            containerColor = Color.Transparent,
                            checkedContainerColor = Color.Transparent,
                            contentColor = contentColor,
                            checkedContentColor = contentColor,
                        ),
                        shape = buttonShape,
                        modifier = Modifier
                            .size(BUTTON_SIZE)
                            .clip(buttonShape)
                            .drawBehind { drawRect(containerColor) }
                            .testTag(OverviewTestTags.filter(filter)),
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = label,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@MedTimerPreview
@Composable
private fun OverviewFilterRowPreview() {
    // Interactive so the highlight animation can be exercised straight from the preview.
    var activeFilters by remember { mutableStateOf(setOf(OverviewFilter.TAKEN, OverviewFilter.RAISED)) }
    MedTimerTheme {
        Surface {
            OverviewFilterRow(
                activeFilters = activeFilters,
                onToggleFilter = { filter ->
                    activeFilters = if (filter in activeFilters) activeFilters - filter else activeFilters + filter
                },
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
