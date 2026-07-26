package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.component.SelectableCard
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.overview.actions.Actions
import com.futsch1.medtimer.feature.ui.overview.actions.Button
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEventContent
import com.futsch1.medtimer.feature.ui.overview.model.OverviewState
import com.futsch1.medtimer.feature.ui.overview.model.StockChange
import com.futsch1.medtimer.feature.ui.overview.model.getImage
import com.futsch1.medtimer.feature.ui.overview.model.toString
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.futsch1.medtimer.core.ui.R as CoreUiR

internal val EVENT_STATE_BUTTON_SIZE = 48.dp
internal val EVENT_STATE_BUTTON_MARGIN = 8.dp

internal val EVENT_SPACING = 20.dp

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
    val context = LocalContext.current
    var showActions by remember { mutableStateOf(false) }
    val containerColor = eventContainerColor(color, isSelected)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            // Reported from the placed coordinates rather than the list's layoutInfo, which
            // exposes target offsets only and so runs ahead of animateItem's placement spring.
            modifier = Modifier.onGloballyPositioned {
                onRailAnchor(it.positionInRoot().y + it.size.height / 2f)
            },
        ) {
            IconButton(
                onClick = { showActions = true },
                modifier = Modifier
                    .testTag(OverviewTestTags.EVENT_STATE_BUTTON)
                    .padding(EVENT_STATE_BUTTON_MARGIN)
                    .size(EVENT_STATE_BUTTON_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                val slideSpec = MaterialTheme.motionScheme.slowSpatialSpec<IntOffset>()
                val fadeSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        slideInHorizontally(slideSpec) { it } + fadeIn(fadeSpec) togetherWith
                                slideOutHorizontally(slideSpec) { -it } + fadeOut(fadeSpec) using
                                SizeTransform(clip = false)
                    },
                    label = "event_state",
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(EVENT_STATE_BUTTON_MARGIN)
                        .clip(CircleShape),
                ) { animatedState ->
                    Icon(
                        painter = painterResource(animatedState.getImage()),
                        contentDescription = animatedState.toString(context),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                actions?.visibleButtons.orEmpty().forEach { button ->
                    DropdownMenuItem(
                        text = { Text(stringResource(button.labelRes)) },
                        onClick = {
                            showActions = false
                            onAction(button)
                        },
                        leadingIcon = {
                            Icon(painterResource(button.iconRes), contentDescription = null)
                        },
                    )
                }
            }
        }

        SelectableCard(
            isSelected = isSelected,
            isInSelectionMode = isInSelectionMode,
            onClick = onClick,
            onToggleSelection = onToggleSelection,
            onEnterSelectionMode = onEnterSelectionMode,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColorFor(containerColor),
            ),
            modifier = Modifier
                .testTag(OverviewTestTags.EVENT_CARD)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp),
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
                    targetState = icon,
                    transitionSpec = {
                        fadeIn(iconFadeSpec) togetherWith fadeOut(iconFadeSpec) using
                                SizeTransform(clip = false) { _, _ -> iconSizeSpec }
                    },
                    label = "medicine_icon",
                ) { animatedIcon ->
                    if (animatedIcon != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                        ) {
                            Icon(
                                bitmap = animatedIcon,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(EVENT_STATE_BUTTON_MARGIN)
                                    .size(32.dp),
                            )
                        }
                    }
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
                color = null,
                icon = previewMedicineIcon(),
                isSelected = false,
                isInSelectionMode = false,
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
private fun eventContainerColor(color: Int?, isSelected: Boolean) = when {
    isSelected -> MaterialTheme.colorScheme.secondaryContainer
    color != null -> Color(color).copy(alpha = 1f)
    else -> MaterialTheme.colorScheme.surfaceContainer
}

/**
 * Picks whichever of onSurface/onPrimary reads better on [background] — medicine colours are
 * user-chosen and can land anywhere on the light/dark range.
 */
@Composable
private fun contentColorFor(background: Color): Color {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    return remember(background, onSurface, onPrimary) {
        val bg = background.toArgb() or -0x1000000
        if (ColorUtils.calculateContrast(onSurface.toArgb(), bg) >
            ColorUtils.calculateContrast(onPrimary.toArgb(), bg)
        ) onSurface else onPrimary
    }
}
