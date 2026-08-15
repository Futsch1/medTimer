package com.futsch1.medtimer.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * A card that doubles as a selection target. Tapping activates the item normally, or toggles its
 * selection while [isInSelectionMode]; long-pressing starts selection mode.
 */
@Composable
fun SelectableCard(
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Transparent,
        label = "selection_border",
    )

    Card(
        colors = colors,
        elevation = elevation,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 3.dp, color = borderColor, shape = CardDefaults.shape)
            .combinedClickable(
                onClick = { if (isInSelectionMode) onToggleSelection() else onClick() },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isInSelectionMode) onToggleSelection() else onEnterSelectionMode()
                },
            ),
        content = content,
    )
}
