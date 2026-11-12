package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.core.ui.time.rememberFormattedTime
import com.futsch1.medtimer.feature.ui.overview.actions.Button
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.futsch1.medtimer.core.ui.R as CoreUiR

/**
 * Lines the header text up with the text inside the cards: past the state button column, which the
 * rail runs through, and then the card's own content padding.
 */
private val GROUP_HEADER_START_PADDING = EVENT_STATE_BUTTON_SIZE + EVENT_STATE_BUTTON_MARGIN * 2 + 12.dp

/** Matches the touch target of the header actions, so a header with and without them is the same height. */
private val GROUP_HEADER_MIN_HEIGHT = 48.dp

/**
 * The time shared by the events of one group of two or more events, shown above the group's card,
 * with the actions for the whole group: `Confirm all` when [showConfirmAll], and an overflow that
 * opens the action menu over [menuButtons] when that list is not empty. A one-event group has no header.
 */
@Composable
fun OverviewGroupHeader(
    timestamp: Long,
    showConfirmAll: Boolean,
    menuButtons: List<Button>,
    onConfirmAll: () -> Unit,
    onMenuAction: (Button) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = GROUP_HEADER_MIN_HEIGHT)
            .padding(start = GROUP_HEADER_START_PADDING),
    ) {
        Text(
            text = rememberFormattedTime(Instant.ofEpochSecond(timestamp)),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(OverviewTestTags.GROUP_TIME),
        )
        Spacer(Modifier.weight(1f))
        if (showConfirmAll) {
            TextButton(onClick = onConfirmAll, modifier = Modifier.testTag(OverviewTestTags.GROUP_CONFIRM_ALL)) {
                Text(stringResource(CoreUiR.string.confirm_all))
            }
        }
        if (menuButtons.isNotEmpty()) {
            GroupMenuButton(buttons = menuButtons, onAction = onMenuAction)
        }
    }
}

@Composable
private fun GroupMenuButton(buttons: List<Button>, onAction: (Button) -> Unit) {
    var showActions by remember { mutableStateOf(false) }
    var anchorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    IconButton(
        onClick = { showActions = true },
        modifier = Modifier
            .testTag(OverviewTestTags.GROUP_MENU)
            .onGloballyPositioned { anchorCoordinates = it },
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.three_dots_vertical),
            contentDescription = stringResource(CoreUiR.string.more_options),
        )
    }

    // The button sits at the end edge, so the arc fans back across the card.
    ArcActionMenu(
        expanded = showActions,
        buttons = buttons,
        anchorCoordinates = anchorCoordinates,
        anchorContent = {
            Icon(
                painter = painterResource(CoreUiR.drawable.three_dots_vertical),
                contentDescription = null,
                modifier = Modifier.clearAndSetSemantics {},
            )
        },
        onDismissRequest = { showActions = false },
        onAction = { button ->
            showActions = false
            onAction(button)
        },
        fanTowardsStart = true,
    )
}

@MedTimerPreview
@Composable
private fun OverviewGroupHeaderPreview() {
    MedTimerTheme {
        Surface {
            OverviewGroupHeader(
                timestamp = LocalDate.of(2026, 5, 28).atTime(9, 0).atZone(ZoneId.systemDefault()).toEpochSecond(),
                showConfirmAll = true,
                menuButtons = listOf(Button.TAKEN, Button.SKIPPED, Button.RESCHEDULE),
                onConfirmAll = {},
                onMenuAction = {},
            )
        }
    }
}
