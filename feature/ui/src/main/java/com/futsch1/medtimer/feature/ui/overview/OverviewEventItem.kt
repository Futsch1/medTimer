package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.component.MedicineSwatch
import com.futsch1.medtimer.core.ui.component.SelectableCard
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.overview.actions.Actions
import com.futsch1.medtimer.feature.ui.overview.actions.Button
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEventContent
import com.futsch1.medtimer.feature.ui.overview.model.OverviewState
import com.futsch1.medtimer.feature.ui.overview.model.StockChange
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.futsch1.medtimer.core.ui.R as CoreUiR

internal val EVENT_STATE_BUTTON_SIZE = 48.dp
internal val EVENT_STATE_BUTTON_MARGIN = 8.dp

internal val EVENT_SPACING = 12.dp

/** Horizontal centre of the state buttons, which the list draws its connecting pills along. */
internal val RAIL_CENTER_X = EVENT_STATE_BUTTON_MARGIN + EVENT_STATE_BUTTON_SIZE / 2
internal val RAIL_PILL_WIDTH = 6.dp
internal val RAIL_PILL_RESTING_HEIGHT = RAIL_PILL_WIDTH * 2

/**
 * Distance a pill keeps from each neighbouring button circle. Derived so that two min-height
 * neighbours produce [RAIL_PILL_RESTING_HEIGHT]; changing the spacing keeps the resting look.
 */
internal val RAIL_INSET =
    (EVENT_SPACING + EVENT_STATE_BUTTON_MARGIN * 2 - RAIL_PILL_RESTING_HEIGHT) / 2

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@SuppressWarnings("kotlin:S107")
fun OverviewEventItem(
    content: OverviewEventContent,
    state: OverviewState,
    color: Int?,
    icon: ImageBitmap?,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    actions: Actions?,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onAction: (Button) -> Unit,
    modifier: Modifier = Modifier,
    onRailAnchor: (Float) -> Unit = {},
) {
    val containerColor = eventContainerColor(isSelected)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.onGloballyPositioned {
                onRailAnchor(it.positionInRoot().y + it.size.height / 2f)
            },
        ) {
            EventStateButton(
                state = state,
                actions = actions,
                onAction = onAction,
            )
        }

        SelectableCard(
            isSelected = isSelected,
            isInSelectionMode = isInSelectionMode,
            onClick = onClick,
            onToggleSelection = onToggleSelection,
            onEnterSelectionMode = onEnterSelectionMode,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier
                .testTag(OverviewTestTags.EVENT_CARD)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
            ) {
                EventContent(
                    content = content,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(OverviewTestTags.EVENT_TEXT),
                )
                val iconFadeSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
                val iconSizeSpec = MaterialTheme.motionScheme.slowSpatialSpec<IntSize>()
                AnimatedContent(
                    targetState = icon to color,
                    transitionSpec = {
                        fadeIn(iconFadeSpec) togetherWith fadeOut(iconFadeSpec) using
                                SizeTransform(clip = false) { _, _ -> iconSizeSpec }
                    },
                    label = "medicine_swatch",
                ) { (animatedIcon, animatedColor) ->
                    MedicineSwatch(icon = animatedIcon, color = animatedColor)
                }
            }
        }
    }
}

private val PREVIEW_TIME: Instant = LocalDate.of(2026, 5, 28).atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant()

// At runtime the icon comes from the medicine icon pack, which previews cannot load.
@Composable
private fun previewMedicineIcon(): ImageBitmap? {
    val context = LocalContext.current
    return remember(context) {
        ContextCompat.getDrawable(context, CoreUiR.drawable.capsule)?.toBitmap()?.asImageBitmap()
    }
}

@MedTimerPreview
@Composable
private fun OverviewEventItemPreview() {
    OverviewEventItemPreviewBase(color = null, isSelected = false)
}

@MedTimerPreview
@Composable
private fun OverviewEventItemVividColorPreview() {
    OverviewEventItemPreviewBase(color = 0xFF00FF00.toInt(), isSelected = false)
}

@MedTimerPreview
@Composable
private fun OverviewEventItemVividBlueColorPreview() {
    OverviewEventItemPreviewBase(color = 0xFF0000FF.toInt(), isSelected = false)
}

@MedTimerPreview
@Composable
private fun OverviewEventItemColorWithoutIconPreview() {
    OverviewEventItemPreviewBase(color = 0xFFFF0000.toInt(), isSelected = false, withIcon = false)
}

@MedTimerPreview
@Composable
private fun OverviewEventItemSelectedPreview() {
    OverviewEventItemPreviewBase(color = 0xFFFF00FF.toInt(), isSelected = true)
}

@Composable
private fun OverviewEventItemPreviewBase(color: Int?, isSelected: Boolean, withIcon: Boolean = true) {
    MedTimerTheme {
        Surface {
            OverviewEventItem(
                content = OverviewEventContent(
                    reminderType = ReminderType.TIME_BASED,
                    time = PREVIEW_TIME,
                    medicineName = "Vitamin D",
                    dose = "1 tablet",
                    takenTime = PREVIEW_TIME.plus(Duration.ofMinutes(42)),
                    interval = Duration.ofMinutes(150),
                    stock = StockChange(before = 12.0, after = 11.0, unit = "pcs"),
                ),
                state = OverviewState.TAKEN,
                color = color,
                icon = previewMedicineIcon().takeIf { withIcon },
                isSelected = isSelected,
                isInSelectionMode = isSelected,
                actions = null,
                onClick = {},
                onToggleSelection = {},
                onEnterSelectionMode = {},
                onAction = {},
            )
        }
    }
}

@Composable
private fun eventContainerColor(isSelected: Boolean) =
    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainer

