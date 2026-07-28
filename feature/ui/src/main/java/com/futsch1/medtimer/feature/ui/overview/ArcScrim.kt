package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/** Peak opacity of the scrim at the anchor, fading out to transparent by its outer radius. */
private const val ARC_SCRIM_PEAK_ALPHA = 0.32f

/** Extra reach past the farthest button before the scrim fades to transparent. */
internal val ARC_SCRIM_BLEED = 32.dp

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
    val spec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(expanded) {
        alpha.animateTo(if (expanded) 1f else 0f, spec)
    }

    Box(
        Modifier
            .drawBehind {
                val radius = size.width / 2f
                if (radius <= 0f) return@drawBehind
                val center = Offset(radius, radius)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            scrimColor.copy(alpha = ARC_SCRIM_PEAK_ALPHA * alpha.value),
                            scrimColor.copy(alpha = 0f),
                        ),
                        center = center,
                        radius = radius,
                    ),
                )
            }
    )
}
