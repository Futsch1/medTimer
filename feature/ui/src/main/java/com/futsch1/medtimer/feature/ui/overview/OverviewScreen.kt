package com.futsch1.medtimer.feature.ui.overview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.ui.MedicineIcons
import com.futsch1.medtimer.core.ui.time.rememberFormattedDate
import com.futsch1.medtimer.feature.ui.overview.actions.Button
import com.futsch1.medtimer.feature.ui.overview.actions.MultipleActions
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import com.futsch1.medtimer.core.ui.R as CoreUiR

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    medicineIcons: MedicineIcons,
    warningsState: OverviewWarningsState,
    onDismissBatteryWarning: () -> Unit,
    onDismissExactRemindersWarning: () -> Unit,
    onEventClick: (OverviewEvent) -> Unit,
    onAction: (Button, List<OverviewEvent>) -> Unit,
    onLogManualDose: () -> Unit,
    topBarActions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val selection = viewModel.selection
    val events = selection.items
    val day = viewModel.day
    val simulatedThrough by viewModel.simulatedThrough.collectAsState()
    val listState = rememberLazyListState()
    var scrolledToNow by remember { mutableStateOf(false) }

    LaunchedEffect(events) {
        if (scrolledToNow || events.isEmpty()) return@LaunchedEffect
        scrolledToNow = true
        val now = LocalTime.now()
        val index = events.indexOfFirst {
            Instant.ofEpochSecond(it.timestamp).atZone(ZoneId.systemDefault()).toLocalTime() >= now
        }
        if (index >= 0) listState.scrollToItem(index)
    }

    BackHandler(enabled = selection.isInSelectionMode) { selection.exitSelectionMode() }

    val title = "${stringResource(CoreUiR.string.tab_overview)} - ${rememberFormattedDate(day, "MMMMy")}"
    Column(modifier.semantics { testTagsAsResourceId = true }) {
        OverviewTopBar(
            title = title,
            isInSelectionMode = selection.isInSelectionMode,
            selectedCount = selection.selectedIds.size,
            selectionActions = remember(selection.selectedItems) {
                MultipleActions(selection.selectedItems)
            },
            onExitSelectionMode = selection::exitSelectionMode,
            onSelectAll = selection::selectAll,
            onSelectionAction = { button ->
                val selected = selection.selectedItems.toList()
                selection.exitSelectionMode()
                onAction(button, selected)
            },
            actions = topBarActions,
        )

        Warnings(
            state = warningsState,
            onDismissBatteryWarning = onDismissBatteryWarning,
            onDismissExactRemindersWarning = onDismissExactRemindersWarning,
        )

        OverviewWeekSelector(
            selectedDay = day,
            rangeEnd = simulatedThrough,
            onDaySelected = viewModel::selectDay,
            modifier = Modifier.fillMaxWidth(),
        )

        OverviewFilterRow(
            activeFilters = viewModel.activeFilters,
            onToggleFilter = viewModel::toggleFilter,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp),
        )

        Box(Modifier.fillMaxSize()) {
            OverviewEventList(
                events = events,
                selection = selection,
                listState = listState,
                medicineIcons = medicineIcons,
                onEventClick = onEventClick,
                onEnterSelectionMode = { event ->
                    selection.enterSelectionMode()
                    if (viewModel.combineNotifications) {
                        viewModel.selectSameTimeEvents(event)
                    } else {
                        selection.toggleSelection(event)
                    }
                },
                onAction = { button, event -> onAction(button, listOf(event)) },
            )

            ExtendedFloatingActionButton(
                onClick = onLogManualDose,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .testTag(OverviewTestTags.LOG_MANUAL_DOSE),
                icon = { Icon(painterResource(CoreUiR.drawable.capsule), contentDescription = null) },
                text = { Text(stringResource(CoreUiR.string.log_additional_dose), maxLines = 2) },
            )
        }
    }
}
