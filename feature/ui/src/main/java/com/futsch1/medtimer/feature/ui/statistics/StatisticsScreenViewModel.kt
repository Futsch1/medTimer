package com.futsch1.medtimer.feature.ui.statistics

import androidx.core.graphics.toColorInt
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

    init {
        val data = persistentDataDataSource.data.value
        _state.activeView = data.activeStatisticsFragment
        _state.analysisDays = data.analysisDays
        loadCharts(data.analysisDays)
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
        loadCharts(days)
    }

    private fun loadCharts(days: Int) {
        viewModelScope.launch(ioDispatcher) {
            val perDay = statisticsProvider.getLastDaysReminders(days)
            val seriesColors = computeSeriesColors(perDay)
            val period = statisticsProvider.getTakenSkippedData(days)
            val total = statisticsProvider.getTakenSkippedData(0)
            _state.charts = ChartsState(
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
    }

    private suspend fun computeSeriesColors(data: MedicinePerDayData): List<Int> {
        val allMedicines = medicineRepository.getAll()
        var colorIndex = 0
        return data.series.map { series ->
            allMedicines
                .firstOrNull { it.name == series.medicineName && it.useColor }
                ?.color
                ?: COLORS[colorIndex++ % COLORS.size]
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
        private val COLORS = intArrayOf(
            "#003f5c".toColorInt(),
            "#2f4b7c".toColorInt(),
            "#665191".toColorInt(),
            "#a05195".toColorInt(),
            "#d45087".toColorInt(),
            "#f95d6a".toColorInt(),
            "#ff7c43".toColorInt(),
            "#ffa600".toColorInt(),
            "#004c6d".toColorInt(),
            "#295d7d".toColorInt(),
            "#436f8e".toColorInt(),
            "#5b829f".toColorInt(),
            "#7295b0".toColorInt(),
            "#89a8c2".toColorInt(),
            "#a1bcd4".toColorInt(),
            "#b8d0e6".toColorInt(),
            "#d0e5f8".toColorInt(),
        )
    }
}
