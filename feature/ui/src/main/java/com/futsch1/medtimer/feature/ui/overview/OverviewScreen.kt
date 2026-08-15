package com.futsch1.medtimer.feature.ui.overview

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.ScreenTestTags
import com.futsch1.medtimer.core.ui.list.SelectionListController
import com.futsch1.medtimer.core.ui.preview.MedTimerPreview
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.core.ui.time.rememberFormattedDate
import com.futsch1.medtimer.feature.reminders.api.SimulatedReminders.Companion.DEFAULT_SIMULATION_DAYS
import com.futsch1.medtimer.feature.ui.overview.actions.Button
import com.futsch1.medtimer.feature.ui.overview.actions.MultipleActions
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEventContent
import com.futsch1.medtimer.feature.ui.overview.model.OverviewState
import com.google.gson.Gson
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.futsch1.medtimer.core.ui.R as CoreUiR

/** Below this width the week selector and filter row don't both fit on one line and stack instead. */
private val NAVIGATION_ROW_MIN_WIDTH = 600.dp

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    onEventClick: (OverviewEvent) -> Unit,
    onAction: (Button, List<OverviewEvent>) -> Unit,
    onLogManualDose: () -> Unit,
    topBarActions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    OverviewScreen(
        state = viewModel.state,
        selection = viewModel.selection,
        onDaySelected = viewModel::selectDay,
        onToggleFilter = viewModel::toggleFilter,
        onDismissBatteryWarning = viewModel::dismissBatteryWarning,
        onDismissExactRemindersWarning = viewModel::dismissExactRemindersWarning,
        onEnterSelectionMode = { event ->
            viewModel.selection.enterSelectionMode()
            if (viewModel.state.combineNotifications) {
                viewModel.selectSameTimeEvents(event)
            } else {
                viewModel.selection.toggleSelection(event)
            }
        },
        onEventClick = onEventClick,
        onAction = onAction,
        onLogManualDose = onLogManualDose,
        topBarActions = topBarActions,
        modifier = modifier,
    )
}

/** Stateless screen — the `@Preview`/test target. Renders purely from its inputs. */
@Composable
@SuppressWarnings("kotlin:S107")
fun OverviewScreen(
    state: OverviewScreenState,
    selection: SelectionListController<OverviewEvent>,
    onDaySelected: (LocalDate) -> Unit,
    onToggleFilter: (OverviewFilter) -> Unit,
    onDismissBatteryWarning: () -> Unit,
    onDismissExactRemindersWarning: () -> Unit,
    onEnterSelectionMode: (OverviewEvent) -> Unit,
    onEventClick: (OverviewEvent) -> Unit,
    onAction: (Button, List<OverviewEvent>) -> Unit,
    onLogManualDose: () -> Unit,
    topBarActions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val events = state.events
    val rangeStart = remember { LocalDate.now().minusYears(3) }
    val rangeEnd = maxOf(state.simulatedThrough, LocalDate.now().plusDays(DEFAULT_SIMULATION_DAYS))

    BackHandler(enabled = selection.isInSelectionMode) { selection.exitSelectionMode() }

    val title = "${stringResource(CoreUiR.string.tab_overview)} - ${rememberFormattedDate(state.day, "MMMMy")}"
    Column(modifier.testTag(ScreenTestTags.OVERVIEW)) {
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
            state = state.warnings,
            onDismissBatteryWarning = onDismissBatteryWarning,
            onDismissExactRemindersWarning = onDismissExactRemindersWarning,
        )

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= NAVIGATION_ROW_MIN_WIDTH) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    OverviewWeekSelector(
                        selectedDay = state.day,
                        rangeEnd = state.simulatedThrough,
                        onDaySelected = onDaySelected,
                        modifier = Modifier.weight(1f),
                    )

                    OverviewFilterRow(
                        activeFilters = state.activeFilters,
                        onToggleFilter = onToggleFilter,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            } else {
                Column {
                    OverviewWeekSelector(
                        selectedDay = state.day,
                        rangeEnd = state.simulatedThrough,
                        onDaySelected = onDaySelected,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OverviewFilterRow(
                        activeFilters = state.activeFilters,
                        onToggleFilter = onToggleFilter,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .overviewDaySwipe { dayOffset ->
                    val target = state.day.plusDays(dayOffset.toLong())
                    if (target in rangeStart..rangeEnd) onDaySelected(target)
                },
        ) {
            OverviewEventList(
                events = events,
                selection = selection,
                onEventClick = onEventClick,
                onEnterSelectionMode = onEnterSelectionMode,
                onAction = { button, event -> onAction(button, listOf(event)) },
                modifier = Modifier.padding(end = 8.dp),
            )

            ExtendedFloatingActionButton(
                onClick = onLogManualDose,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag(OverviewTestTags.LOG_MANUAL_DOSE),
                icon = { Icon(painterResource(CoreUiR.drawable.capsule), contentDescription = null) },
                text = { Text(stringResource(CoreUiR.string.log_additional_dose), maxLines = 2) },
            )
        }
    }
}

private val PREVIEW_TIME: Instant = LocalDate.of(2026, 5, 28).atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant()

private class PreviewOverviewEvent(
    override val id: Int,
    override val timestamp: Long,
    override val content: OverviewEventContent,
    override val state: OverviewState,
    preferencesDataSource: PreferencesDataSource,
) : OverviewEvent(preferencesDataSource) {
    override val icon = 0
    override val color: Int? = null
    override val reminderId = id
    override val cannotSkipMedicine = false
}

@Composable
private fun rememberPreviewEvents(): List<OverviewEvent> {
    val context = LocalContext.current
    val preferencesDataSource = remember {
        val prefs = context.getSharedPreferences("overview_screen_preview", Context.MODE_PRIVATE)
        PreferencesDataSource(prefs, CoroutineScope(Dispatchers.Unconfined), Gson())
    }
    return remember {
        listOf(
            PreviewOverviewEvent(
                id = 1,
                timestamp = PREVIEW_TIME.epochSecond,
                content = OverviewEventContent(
                    reminderType = ReminderType.TIME_BASED,
                    time = PREVIEW_TIME,
                    medicineName = "Vitamin D",
                    dose = "1 tablet",
                    takenTime = PREVIEW_TIME.plus(Duration.ofMinutes(42)),
                ),
                state = OverviewState.TAKEN,
                preferencesDataSource = preferencesDataSource,
            ),
            PreviewOverviewEvent(
                id = 2,
                timestamp = PREVIEW_TIME.plus(Duration.ofHours(4)).epochSecond,
                content = OverviewEventContent(
                    reminderType = ReminderType.TIME_BASED,
                    time = PREVIEW_TIME.plus(Duration.ofHours(4)),
                    medicineName = "Ibuprofen",
                    dose = "2 tablets",
                ),
                state = OverviewState.RAISED,
                preferencesDataSource = preferencesDataSource,
            ),
            PreviewOverviewEvent(
                id = 3,
                timestamp = PREVIEW_TIME.plus(Duration.ofHours(8)).epochSecond,
                content = OverviewEventContent(
                    reminderType = ReminderType.TIME_BASED,
                    time = PREVIEW_TIME.plus(Duration.ofHours(8)),
                    medicineName = "Antibiotic",
                    dose = "1 capsule",
                ),
                state = OverviewState.SKIPPED,
                preferencesDataSource = preferencesDataSource,
            ),
        )
    }
}

@MedTimerPreview
@Composable
private fun OverviewScreenPreview() {
    val state = remember { MutableOverviewScreenState() }
    state.events = rememberPreviewEvents().toPersistentList()
    val selection = remember { SelectionListController<OverviewEvent> { it.id } }
    MedTimerTheme {
        Surface {
            OverviewScreen(
                state = state,
                selection = selection,
                onDaySelected = {},
                onToggleFilter = {},
                onDismissBatteryWarning = {},
                onDismissExactRemindersWarning = {},
                onEnterSelectionMode = {},
                onEventClick = {},
                onAction = { _, _ -> },
                onLogManualDose = {},
                topBarActions = {},
            )
        }
    }
}
