package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.overview.actions.Button
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/** Horizontal reach of the arc at its widest (the middle button(s), where the bulge peaks). */
private val ARC_RADIUS_X = 48.dp

/**
 * Fixed the vertical gap between adjacent buttons' centers.
 * Linear rather than angle-derived:
 * equal angle steps on an ellipse give unequal gaps (largest near 0°, smallest near ±90°).
 */
private val ARC_Y_STEP = 48.dp

/** Angular step shaping the horizontal bulge only (via cosine); see [ARC_Y_STEP] for vertical spacing. */
private const val ARC_STEP_ANGLE_DEG = 45f

private val ARC_ENTER_STAGGER_DELAY = 70.milliseconds
private val ARC_EXIT_STAGGER_DELAY = 40.milliseconds
private val ARC_EXIT_DURATION = 220.milliseconds

/** Display order top-to-bottom; buttons not visible for a given event are simply skipped. */
private val ARC_BUTTON_TOP_TO_BOTTOM_ORDER = listOf(
    Button.SKIPPED,
    Button.TAKEN,
    Button.ACKNOWLEDGED,
    Button.RERAISE,
    Button.RESCHEDULE,
    Button.DELETE,
)

/**
 * Fans [buttons] out along an arc to the right of the anchor (mirrored left in RTL), over a
 * radial scrim radiating from that same point.
 * Buttons reveal bottom-most first and collapse in reverse before the popup unmounts.
 * Runs in a window-sized popup, with everything positioned against [anchorCoordinates] — the state
 * button's own measured position — rather than by anchoring the popup window itself.
 */
@Composable
internal fun ArcActionMenu(
    expanded: Boolean,
    buttons: List<Button>,
    anchorCoordinates: LayoutCoordinates?,
    onDismissRequest: () -> Unit,
    onAction: (Button) -> Unit,
) {
    if (buttons.isEmpty()) return

    var popupVisible by remember { mutableStateOf(false) }
    var revealedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(expanded) {
        if (expanded) {
            popupVisible = true
            withFrameNanos {}
            for (index in buttons.indices) {
                revealedCount = index + 1
                delay(ARC_ENTER_STAGGER_DELAY)
            }
        } else if (popupVisible) {
            for (index in buttons.indices.reversed()) {
                revealedCount = index
                delay(ARC_EXIT_STAGGER_DELAY)
            }
            delay(ARC_EXIT_DURATION)
            popupVisible = false
        }
    }

    if (popupVisible && anchorCoordinates != null) {
        val anchorOnScreen = remember(anchorCoordinates) {
            val position = anchorCoordinates.positionOnScreen()
            Offset(
                position.x + anchorCoordinates.size.width / 2f,
                position.y + anchorCoordinates.size.height / 2f,
            )
        }

        Popup(
            popupPositionProvider = WindowOriginPositionProvider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true, usePlatformDefaultWidth = false),
        ) {
            // The popup lives in its own window, whose origin need not line up with the host
            // window's (system bar insets shift it). Screen coordinates are the one space both
            // agree on, so the anchor is translated into popup-local space through them.
            var contentOnScreen by remember { mutableStateOf(Offset.Zero) }

            Box(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { contentOnScreen = it.positionOnScreen() }
            ) {
                ArcActionButtons(
                    buttons = buttons,
                    revealedCount = revealedCount,
                    expanded = expanded,
                    anchorCenter = anchorOnScreen - contentOnScreen,
                    onDismissRequest = onDismissRequest,
                    onAction = onAction,
                )
            }
        }
    }
}

/**
 * Pins the popup window to the host window's origin so it spans the whole screen, letting the
 * content position itself against [LayoutCoordinates.positionOnScreen] coordinates directly.
 *
 * Anchoring the window itself is not viable here: the scrim reaches well past the screen edge next
 * to a left-aligned anchor, and WindowManager shoves an oversized window back on-screen, dragging
 * the buttons with it.
 */
private object WindowOriginPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ) = IntOffset.Zero
}

/**
 * The arc's visual content,
 * factored out from [ArcActionMenu] so previews can render it fully revealed without going through the popup/stagger lifecycle.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArcActionButtons(
    buttons: List<Button>,
    revealedCount: Int,
    expanded: Boolean,
    anchorCenter: Offset,
    onDismissRequest: () -> Unit,
    onAction: (Button) -> Unit,
) {
    val density = LocalDensity.current
    val radiusXPx = with(density) { ARC_RADIUS_X.toPx() }
    val yStepPx = with(density) { ARC_Y_STEP.toPx() }
    val orderedButtons = remember(buttons) { buttons.sortedByDescending(ARC_BUTTON_TOP_TO_BOTTOM_ORDER::indexOf) }
    val angles = remember(orderedButtons.size) { arcAngles(orderedButtons.size) }
    val yPositions = remember(orderedButtons.size, yStepPx) { arcYPositions(orderedButtons.size, yStepPx) }

    Layout(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest,
            ),
        content = {
            ArcScrim(expanded)
            orderedButtons.forEachIndexed { index, button ->
                AnimatedVisibility(
                    visible = index < revealedCount,
                    enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
                ) {
                    SmallFloatingActionButton(onClick = { onAction(button) }) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Icon(painterResource(button.iconRes), contentDescription = null)
                            Text(
                                stringResource(button.labelRes),
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val targets = arcTargets(measurables.drop(1), angles, yPositions, radiusXPx)
        val scrimRadius = hypot(
            targets.arcWidth().toFloat(),
            arcHalfHeight(yPositions, targets).toFloat(),
        ) + ARC_SCRIM_BLEED.toPx()
        val scrimSide = (scrimRadius * 2f).roundToInt().coerceAtLeast(0)
        val scrim = measurables.first().measure(Constraints.fixed(scrimSide, scrimSide))

        layout(constraints.maxWidth, constraints.maxHeight) {
            scrim.place(
                (anchorCenter.x - scrimRadius).roundToInt(),
                (anchorCenter.y - scrimRadius).roundToInt(),
            )
            placeArcTargets(targets, anchorCenter, layoutDirection)
        }
    }
}


/** Each button's measured [placeable], with its arc anchor point (the near/start edge it grows from). */
private data class ArcTarget(val startX: Float, val centerY: Float, val placeable: Placeable)

private fun arcTargets(
    measurables: List<Measurable>,
    angles: List<Float>,
    yPositionsPx: List<Float>,
    radiusXPx: Float,
): List<ArcTarget> =
    measurables.mapIndexed { index, measurable ->
        val angleRad = Math.toRadians(angles[index].toDouble())
        val startX = (radiusXPx * cos(angleRad)).toFloat()
        ArcTarget(startX, yPositionsPx[index], measurable.measure(Constraints()))
    }

/** Tight-fit width around the buttons — not doubled, since startX is always >= 0 (no content on the far side). */
private fun List<ArcTarget>.arcWidth(): Int =
    (maxOfOrNull { it.startX + it.placeable.width } ?: 0f).roundToInt().coerceAtLeast(0)

/**
 * Vertical extent from [yPositions] — the full, fixed set of target offsets for all of this
 * button group's members, not just whichever ones happen to be composed on a given frame (which
 * shrinks during the staggered reveal/hide, and would otherwise make the popup's own centering
 * drift vertically as buttons enter/exit). Each button's own height is effectively constant
 * (independent of label length), so a reference height from whatever is currently measured is
 * enough to pair with the full position list.
 */
private fun arcHalfHeight(yPositions: List<Float>, targets: List<ArcTarget>): Int {
    val maxAbsY = yPositions.maxOfOrNull { abs(it) } ?: 0f
    val referenceHeight = targets.maxOfOrNull { it.placeable.height } ?: 0
    return (maxAbsY + referenceHeight / 2f).roundToInt().coerceAtLeast(0)
}

/**
 * Places each button relative to [anchorCenter], mirroring about the anchor in RTL. `placeRelative`
 * is deliberately avoided: the container spans the whole window, so it would mirror about the
 * screen's centre rather than the anchor's.
 */
private fun Placeable.PlacementScope.placeArcTargets(
    targets: List<ArcTarget>,
    anchorCenter: Offset,
    layoutDirection: LayoutDirection,
) {
    targets.forEach { (startX, centerY, placeable) ->
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            anchorCenter.x + startX
        } else {
            anchorCenter.x - startX - placeable.width
        }
        val y = (anchorCenter.y + centerY).roundToInt() - placeable.height / 2
        placeable.place(x.roundToInt(), y)
    }
}

/** Evenly spaced angles (degrees), [ARC_STEP_ANGLE_DEG] apart, centred on 0° — shapes the horizontal bulge only. */
private fun arcAngles(count: Int): List<Float> {
    if (count == 1) return listOf(0f)
    val half = ARC_STEP_ANGLE_DEG * (count - 1) / 2f
    return List(count) { index -> -half + ARC_STEP_ANGLE_DEG * index }
}

/** Evenly spaced Y positions (px), [yStepPx] apart, ascending from bottom-most (largest Y) to top-most. */
private fun arcYPositions(count: Int, yStepPx: Float): List<Float> {
    if (count == 1) return listOf(0f)
    val half = yStepPx * (count - 1) / 2f
    return List(count) { index -> half - yStepPx * index }
}

@Preview(widthDp = 360, heightDp = 360, name = "ArcActionMenu —2 buttons")
@Composable
private fun ArcActionMenuTwoButtonsPreview() {
    ArcActionMenuPreview(listOf(Button.TAKEN, Button.SKIPPED))
}

@Preview(widthDp = 360, heightDp = 360, name = "ArcActionMenu —3 buttons")
@Composable
private fun ArcActionMenuThreeButtonsPreview() {
    ArcActionMenuPreview(listOf(Button.TAKEN, Button.RERAISE, Button.DELETE))
}

@Preview(widthDp = 360, heightDp = 360, name = "ArcActionMenu —4 buttons")
@Composable
private fun ArcActionMenuFourButtonsPreview() {
    ArcActionMenuPreview(listOf(Button.TAKEN, Button.SKIPPED, Button.RERAISE, Button.DELETE))
}

@Composable
private fun ArcActionMenuPreview(buttons: List<Button>) {
    MedTimerTheme {
        Surface(Modifier.fillMaxSize()) {
            // Stands in for the state button's position, which at runtime comes from the anchor.
            val anchorCenter = with(LocalDensity.current) { Offset(48.dp.toPx(), 180.dp.toPx()) }
            ArcActionButtons(
                buttons = buttons,
                revealedCount = buttons.size,
                expanded = true,
                anchorCenter = anchorCenter,
                onDismissRequest = {},
                onAction = {},
            )
        }
    }
}
