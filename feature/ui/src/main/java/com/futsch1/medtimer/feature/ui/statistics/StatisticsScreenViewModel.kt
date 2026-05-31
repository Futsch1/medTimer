package com.futsch1.medtimer.feature.ui.statistics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.TagRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.core.ui.component.SortableTableCell
import com.futsch1.medtimer.core.ui.component.SortableTableRow
import com.futsch1.medtimer.core.ui.filter.TagEventFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsScreenViewModel @Inject constructor(
    private val statisticsProvider: StatisticsProvider,
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val tagRepository: TagRepository,
    private val persistentDataDataSource: PersistentDataDataSource,
    private val tagEventFilter: TagEventFilter,
    private val timeFormatter: TimeFormatter,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStatisticsScreenState()
    val state: StatisticsScreenState get() = _state

    private val analysisDays = MutableStateFlow(persistentDataDataSource.data.value.analysisDays)

    init {
        val data = persistentDataDataSource.data.value
        _state.activeView = data.activeStatisticsFragment
        _state.analysisDays = data.analysisDays
        observeCharts()
        observeTableRows()
    }

    fun onSelectView(view: StatisticFragment) {
        _state.activeView = view
        persistentDataDataSource.setActiveStatisticsFragment(view)
    }

    fun onSelectRange(days: Int) {
        if (days == _state.analysisDays) return
        _state.analysisDays = days
        persistentDataDataSource.setAnalysisDays(days)
        analysisDays.value = days
    }

    private fun observeCharts() {
        // Recompute whenever the reminder events change or the selected range changes — same reactive
        // pattern as observeTableRows, so adding an event refreshes the charts without a manual reload.
        viewModelScope.launch {
            combine(
                reminderEventRepository.getAllFlow(0, ReminderEvent.statusValuesTakenOrSkipped),
                analysisDays,
            ) { _, days -> buildChartsState(days) }
                .flowOn(ioDispatcher)
                .collect { _state.charts = it }
        }
    }

    private suspend fun buildChartsState(days: Int): ChartsState {
        val perDay = statisticsProvider.getLastDaysReminders(days)
        val seriesColors = computeSeriesColors(perDay)
        val period = statisticsProvider.getTakenSkippedData(days)
        val total = statisticsProvider.getTakenSkippedData(0)
        return ChartsState(
            perDay = perDay,
            dayLabels = perDay.epochDays.map { timeFormatter.daysSinceEpochToDateString(it) }.toImmutableList(),
            seriesColors = seriesColors.toImmutableList(),
            takenPeriod = period.taken,
            skippedPeriod = period.skipped,
            takenTotal = total.taken,
            skippedTotal = total.skipped,
            days = days,
        )
    }

    private suspend fun computeSeriesColors(data: MedicinePerDayData): List<Int> {
        val allMedicines = medicineRepository.getAll()
        var colorIndex = 0
        return data.series.map { series ->
            allMedicines
                .firstOrNull { it.name == series.medicineName && it.useColor }
                ?.color
                ?: COLORS[colorIndex++ % COLORS.size].toArgb()
        }
    }

    private fun observeTableRows() {
        viewModelScope.launch {
            combine(
                reminderEventRepository.getAllFlow(0, ReminderEvent.statusValuesTakenOrSkipped),
                persistentDataDataSource.data,
                tagRepository.getAllFlow(),
            ) { events, persistent, tags ->
                val selectedTagIds = persistent.filterTags.mapNotNull { it.toIntOrNull() }.toSet()
                tagEventFilter.filter(events, selectedTagIds, tags).map { it.toTableRow() }.toImmutableList()
            }
                .flowOn(ioDispatcher)
                .collect { _state.tableRows = it }
        }
    }

    private fun ReminderEvent.toTableRow(): SortableTableRow {
        val takenText = if (status == ReminderEvent.ReminderStatus.TAKEN) {
            timeFormatter.toDateTimeString(processedTimestamp)
        } else {
            "-"
        }
        return SortableTableRow(
            id = reminderEventId.toLong(),
            cells = persistentListOf(
                SortableTableCell(takenText, processedTimestamp),
                SortableTableCell(medicineName),
                SortableTableCell(amount),
                SortableTableCell(timeFormatter.toDateTimeString(remindedTimestamp), remindedTimestamp),
            ),
        )
    }

    companion object {
        private val COLORS = listOf(
            Color(0xFF003F5C),
            Color(0xFF2F4B7C),
            Color(0xFF665191),
            Color(0xFFA05195),
            Color(0xFFD45087),
            Color(0xFFF95D6A),
            Color(0xFFFF7C43),
            Color(0xFFFFA600),
            Color(0xFF004C6D),
            Color(0xFF295D7D),
            Color(0xFF436F8E),
            Color(0xFF5B829F),
            Color(0xFF7295B0),
            Color(0xFF89A8C2),
            Color(0xFFA1BCD4),
            Color(0xFFB8D0E6),
            Color(0xFFD0E5F8),
        )
    }
}
