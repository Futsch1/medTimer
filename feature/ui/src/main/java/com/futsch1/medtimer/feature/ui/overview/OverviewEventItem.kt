package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.component.SelectableCard
import com.futsch1.medtimer.feature.ui.overview.actions.Actions
import com.futsch1.medtimer.feature.ui.overview.actions.Button
import com.futsch1.medtimer.feature.ui.overview.model.EventPosition
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import com.futsch1.medtimer.feature.ui.overview.model.getImage
import com.futsch1.medtimer.feature.ui.overview.model.toString

private val TIMELINE_WIDTH = 16.dp
private val STATE_BUTTON_SIZE = 48.dp

/**
 * One timeline row: the state button on a connecting rail, and the event's own card beside it. The
 * rail segments are hidden at the ends of the list so the timeline starts and stops with the events.
 */
@Composable
fun OverviewEventItem(
    event: OverviewEvent,
    icon: ImageBitmap?,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    actions: Actions?,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onAction: (Button) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showActions by remember { mutableStateOf(false) }
    val containerColor = eventContainerColor(event, isSelected)
    val railColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxHeight()) {
                RailSegment(
                    visible = event.eventPosition != EventPosition.FIRST && event.eventPosition != EventPosition.ONLY,
                    color = railColor,
                )
                RailSegment(
                    visible = event.eventPosition != EventPosition.LAST && event.eventPosition != EventPosition.ONLY,
                    color = railColor,
                )
            }
            IconButton(
                onClick = { showActions = true },
                modifier = Modifier
                    .testTag(OverviewTestTags.EVENT_STATE_BUTTON)
                    .padding(start = 8.dp)
                    .size(STATE_BUTTON_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    painter = painterResource(event.state.getImage()),
                    contentDescription = event.state.toString(context),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
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
                .padding(start = 4.dp, top = 1.dp, bottom = 1.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Text(
                    text = event.text.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(OverviewTestTags.EVENT_TEXT),
                )
                // A medicine's iconId indexes the icon pack, not the app's drawables, and 0 means
                // "no icon" — the caller resolves it to a bitmap.
                if (icon != null) {
                    Icon(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 2.dp, end = 4.dp)
                            .size(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.RailSegment(visible: Boolean, color: Color) {
    Box(
        Modifier
            .weight(1f)
            .width(TIMELINE_WIDTH)
            .background(if (visible) color else Color.Transparent)
    )
}

@Composable
private fun eventContainerColor(event: OverviewEvent, isSelected: Boolean) = when {
    isSelected -> MaterialTheme.colorScheme.secondaryContainer
    event.color != null -> Color(event.color!!).copy(alpha = 1f)
    else -> MaterialTheme.colorScheme.surfaceContainerHighest
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
