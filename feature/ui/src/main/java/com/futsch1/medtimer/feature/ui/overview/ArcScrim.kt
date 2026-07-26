package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Peak opacity of the scrim at the anchor, fading out to transparent by its outer radius. */
private const val ARC_SCRIM_PEAK_ALPHA = 0.32f

/** Extra reach past the farthest button before the scrim fades to transparent. */
internal val ARC_SCRIM_BLEED = 32.dp

/** Radius kept fully clear of scrim, so the state button underneath reads as sitting on top. */
private val ARC_SCRIM_HOLE_RADIUS = EVENT_STATE_BUTTON_SIZE / 2

/** Softens the hole's rim, which a hard cut-out would otherwise outline against the glow. */
private val ARC_SCRIM_HOLE_FEATHER = 12.dp

/**
 * Diffused glow radiating from the anchor, sized to the arc by its parent and drawn to fill
 * whatever bounds it is given.
 *
 * A composable of its own so that its per-frame alpha animation invalidates only itself, rather
 * than the scope hosting the buttons' own transitions.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ArcScrim(expanded: Boolean) {
    val scrimColor = MaterialTheme.colorScheme.scrim
    val alpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "arc_scrim_alpha",
    )

    Box(
        Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                val radius = size.width / 2f
                if (radius <= 0f) return@drawBehind
                val center = Offset(radius, radius)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            scrimColor.copy(alpha = ARC_SCRIM_PEAK_ALPHA * alpha),
                            scrimColor.copy(alpha = 0f),
                        ),
                        center = center,
                        radius = radius,
                    ),
                )

                val holeRadius = ARC_SCRIM_HOLE_RADIUS.toPx()
                val featheredRadius = holeRadius + ARC_SCRIM_HOLE_FEATHER.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to Color.Black,
                        holeRadius / featheredRadius to Color.Black,
                        1f to Color.Transparent,
                        center = center,
                        radius = featheredRadius,
                    ),
                    radius = featheredRadius,
                    center = center,
                    blendMode = BlendMode.DstOut,
                )
            }
    )
}
