package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.futsch1.medtimer.core.ui.component.MedTimerTopAppBar
import com.futsch1.medtimer.feature.ui.overview.actions.Actions
import com.futsch1.medtimer.feature.ui.overview.actions.Button
import com.futsch1.medtimer.core.ui.R as CoreUiR

object OverviewTestTags {
    const val SELECTION_BAR = "overview_selection_bar"
    const val ACTION_MENU = "overview_action_menu"
    const val EVENT_LIST = "overview_event_list"
    const val EVENT_CARD = "overview_event_card"
    const val EVENT_STATE_BUTTON = "overview_event_state"
    const val EVENT_TEXT = "overview_event_text"

    fun day(date: java.time.LocalDate) = "overview_day_$date"

    fun filter(filter: com.futsch1.medtimer.core.domain.model.OverviewFilter) = "overview_filter_${filter.name}"
    const val LOG_MANUAL_DOSE = "overview_log_manual_dose"
}

/**
 * Overview's bar, which swaps to a contextual one while events are selected. The two are crossfaded
 * rather than swapped outright so the surface colour change reads as a mode change.
 */
@Composable
fun OverviewTopBar(
    title: String,
    isInSelectionMode: Boolean,
    selectedCount: Int,
    selectionActions: Actions?,
    onExitSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectionAction: (Button) -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    AnimatedContent(
        targetState = isInSelectionMode,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "overview_top_bar",
    ) { selectionMode ->
        if (selectionMode) {
            OverviewSelectionTopBar(
                selectedCount = selectedCount,
                selectionActions = selectionActions,
                onExitSelectionMode = onExitSelectionMode,
                onSelectAll = onSelectAll,
                onSelectionAction = onSelectionAction,
            )
        } else {
            MedTimerTopAppBar(title = title, actions = actions)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewSelectionTopBar(
    selectedCount: Int,
    selectionActions: Actions?,
    onExitSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectionAction: (Button) -> Unit,
) {
    TopAppBar(
        title = { Text(selectedCount.toString()) },
        modifier = Modifier.testTag(OverviewTestTags.SELECTION_BAR),
        navigationIcon = {
            IconButton(onClick = onExitSelectionMode) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.close),
                    contentDescription = stringResource(CoreUiR.string.close),
                )
            }
        },
        actions = {
            Button.entries.forEach { button ->
                AnimatedVisibility(visible = selectionActions?.visibleButtons?.contains(button) == true) {
                    IconButton(onClick = { onSelectionAction(button) }) {
                        Icon(
                            painter = painterResource(button.iconRes),
                            contentDescription = stringResource(button.labelRes),
                        )
                    }
                }
            }
            IconButton(onClick = onSelectAll) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.check2_all),
                    contentDescription = stringResource(CoreUiR.string.select_all),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        windowInsets = WindowInsets(0),
    )
}
