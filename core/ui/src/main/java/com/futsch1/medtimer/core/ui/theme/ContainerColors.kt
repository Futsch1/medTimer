package com.futsch1.medtimer.core.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/**
 * Picks whichever of onSurface/onPrimary reads better on [background] — medicine colours are
 * user-chosen and can land anywhere on the light/dark range.
 */
@Composable
fun readableContentColorFor(background: Color): Color {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    return remember(background, onSurface, onPrimary) {
        val bg = background.toArgb() or -0x1000000
        if (ColorUtils.calculateContrast(onSurface.toArgb(), bg) >
            ColorUtils.calculateContrast(onPrimary.toArgb(), bg)
        ) onSurface else onPrimary
    }
}

/**
 * Lightening a dark container needs a larger sRGB step than darkening a light one to read as the
 * same separation. Fitted so that masking surfaceContainer reproduces surfaceContainerHighest in
 * both baseline schemes — see ContainerColorsTest.
 */
private const val LIGHTENING_MASK_ALPHA = 0.11f
private const val DARKENING_MASK_ALPHA = 0.06f

/**
 * A translucent veil of [contentColor] that seats an element on whatever container it is drawn
 * over, without needing to know that container's colour.
 *
 * Content colours are bimodal — near-black or near-white — so the veil's own luminance says
 * whether it will lighten or darken what is behind it.
 */
fun containerMask(contentColor: Color): Color = contentColor.copy(
    alpha = if (contentColor.luminance() > 0.5f) LIGHTENING_MASK_ALPHA else DARKENING_MASK_ALPHA
)

/** [containerMask] of the surrounding content colour. */
@Composable
fun containerMask(): Color = containerMask(LocalContentColor.current)
