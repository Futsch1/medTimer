package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
import com.futsch1.medtimer.feature.ui.overview.model.OverviewState
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.time.Duration
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
    Button.TAKEN,
    Button.SKIPPED,
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
 *
 * [anchorContent] restates the anchor's own visual centred on that point and above the scrim, so
 * it keeps reading as undimmed without the scrim having to cut a hole around it; see [ArcScrim].
 */
@Composable
internal fun ArcActionMenu(
    expanded: Boolean,
    buttons: List<Button>,
    anchorCoordinates: LayoutCoordinates?,
    anchorContent: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    onAction: (Button) -> Unit,
) {
    if (buttons.isEmpty()) return

    var popupVisible by remember { mutableStateOf(false) }
    var revealedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(expanded) {
        val motionScale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        suspend fun pause(duration: Duration) = delay(duration * motionScale.toDouble())

        if (expanded) {
            popupVisible = true
            withFrameNanos {}
            for (index in buttons.indices) {
                revealedCount = index + 1
                pause(ARC_ENTER_STAGGER_DELAY)
            }
        } else if (popupVisible) {
            for (index in buttons.indices.reversed()) {
                revealedCount = index
                pause(ARC_EXIT_STAGGER_DELAY)
            }
            pause(ARC_EXIT_DURATION)
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
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .clipToBounds()
                    .onGloballyPositioned { contentOnScreen = it.positionOnScreen() }
            ) {
                ArcActionButtons(
                    buttons = buttons,
                    revealedCount = revealedCount,
                    expanded = expanded,
                    anchorCenter = anchorOnScreen - contentOnScreen,
                    anchorContent = anchorContent,
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
    anchorContent: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    onAction: (Button) -> Unit,
) {
    val density = LocalDensity.current
    val radiusXPx = with(density) { ARC_RADIUS_X.toPx() }
    val yStepPx = with(density) { ARC_Y_STEP.toPx() }
    val orderedButtons = remember(buttons) { buttons.sortedByDescending(ARC_BUTTON_TOP_TO_BOTTOM_ORDER::indexOf) }

    Layout(
        modifier = Modifier
            .fillMaxSize()
            .testTag(OverviewTestTags.ACTION_MENU)
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
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow)),
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
            anchorContent()
        },
    ) { measurables, constraints ->
        val anchorMeasurable = measurables.last()
        val placeables = measurables.subList(1, measurables.lastIndex).map { it.measure(Constraints()) }

        val slots = arcSlots(
            count = orderedButtons.size,
            shift = arcSlotShift(
                count = orderedButtons.size,
                stepPx = yStepPx,
                buttonHeight = placeables.maxOfOrNull { it.height } ?: 0,
                anchorY = anchorCenter.y,
                availableHeight = constraints.maxHeight,
            ),
        )
        val targets = placeables.mapIndexed { index, placeable ->
            ArcTarget(arcStartX(slots[index], radiusXPx), slots[index] * yStepPx, placeable)
        }
        val scrimRadius = hypot(
            targets.arcWidth().toFloat(),
            arcHalfHeight(slots.map { it * yStepPx }, targets).toFloat(),
        ) + ARC_SCRIM_BLEED.toPx()
        val scrimSide = (scrimRadius * 2f).roundToInt().coerceAtLeast(0)
        val scrim = measurables.first().measure(Constraints.fixed(scrimSide, scrimSide))
        val anchor = anchorMeasurable.measure(Constraints())

        layout(constraints.maxWidth, constraints.maxHeight) {
            scrim.place(
                (anchorCenter.x - scrimRadius).roundToInt(),
                (anchorCenter.y - scrimRadius).roundToInt(),
            )
            placeArcTargets(targets, anchorCenter, layoutDirection)
            anchor.place(
                (anchorCenter.x - anchor.width / 2f).roundToInt(),
                (anchorCenter.y - anchor.height / 2f).roundToInt(),
            )
        }
    }
}


/** Each button's measured [placeable], with its arc anchor point (the near/start edge it grows from). */
private data class ArcTarget(val startX: Float, val centerY: Float, val placeable: Placeable)

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

/**
 * Each button's position along the arc, in whole steps from the anchor — positive below it,
 * negative above — ordered bottom-most first. Both the vertical spacing and the horizontal bulge
 * derive from this, so a slot is one full position on the arc rather than a free offset.
 *
 * [shift] slides the whole group along the arc without changing where the arc is centred.
 */
private fun arcSlots(count: Int, shift: Int): List<Float> =
    List(count) { index -> (count - 1) / 2f - index + shift }

/**
 * How far to slide the group so no button falls off the top or bottom edge, in whole slots.
 *
 * Whole slots rather than a free translation: every button stays on a real arc position, so the
 * bulge still peaks level with the anchor and the group simply leaves empty slots at the crowded
 * end. Returns 0 when the group cannot fit either way, which keeps it centred on the anchor.
 */
private fun arcSlotShift(
    count: Int,
    stepPx: Float,
    buttonHeight: Int,
    anchorY: Float,
    availableHeight: Int,
): Int {
    if (stepPx <= 0f) return 0
    val halfButton = buttonHeight / 2f
    val outermost = (count - 1) / 2f
    val lowest = ceil((halfButton - anchorY) / stepPx + outermost)
    val highest = floor((availableHeight - halfButton - anchorY) / stepPx - outermost)
    if (lowest > highest) return 0
    return 0f.coerceIn(lowest, highest).toInt()
}

/**
 * Horizontal reach of a [slot], bulging widest level with the anchor. The angle is clamped to a
 * quarter turn so slots far from the anchor flatten against it rather than curving back behind it.
 */
private fun arcStartX(slot: Float, radiusXPx: Float): Float {
    val angle = (ARC_STEP_ANGLE_DEG * slot).coerceIn(-90f, 90f)
    return (radiusXPx * cos(Math.toRadians(angle.toDouble()))).toFloat()
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
                anchorContent = { EventStateButtonFace(OverviewState.RAISED) },
                onDismissRequest = {},
                onAction = {},
            )
        }
    }
}
