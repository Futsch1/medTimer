package com.futsch1.medtimer.core.ui.component

import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.theme.containerMask
import com.futsch1.medtimer.core.ui.theme.readableContentColorFor

private val SWATCH_SIZE = 48.dp
private val SWATCH_ICON_SIZE = 32.dp

/**
 * The leading marker for a medicine: its color, its icon, or both.
 *
 * The color lives here rather than on the surrounding card because the same hue that reads as a
 * deliberate accent at this size is garish flooded across a whole row. Renders nothing when there
 * is neither an icon nor a color, so plain medicines keep a flush left edge.
 */
@Composable
fun MedicineSwatch(
    icon: ImageBitmap?,
    @ColorInt color: Int?,
    modifier: Modifier = Modifier,
) {
    if (icon == null && color == null) return

    val background = if (color != null) Color(color) else containerMask()
    val contentColor =
        if (color != null) readableContentColorFor(background) else LocalContentColor.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(SWATCH_SIZE)
            .clip(CardDefaults.shape)
            .background(background),
    ) {
        if (icon != null) {
            Icon(
                bitmap = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(SWATCH_ICON_SIZE),
            )
        }
    }
}
