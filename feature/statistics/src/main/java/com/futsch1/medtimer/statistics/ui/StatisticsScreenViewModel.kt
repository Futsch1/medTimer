package com.futsch1.medtimer.statistics.ui

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.statusValuesWithoutDeletedAndAcknowledged
import com.futsch1.medtimer.statistics.domain.AnalysisDays
import com.futsch1.medtimer.statistics.domain.AnalysisDaysPreference
import com.futsch1.medtimer.statistics.domain.GetCalendarEventsUseCase
import com.futsch1.medtimer.statistics.domain.MedicinePerDaySeries
import com.futsch1.medtimer.statistics.domain.StatisticsProvider
import com.futsch1.medtimer.statistics.domain.StatisticsTabPreference
import com.futsch1.medtimer.statistics.domain.StatisticsTabType
import com.futsch1.medtimer.statistics.model.MedicinePerDayData
import com.futsch1.medtimer.statistics.model.MedicineSeriesData
import com.futsch1.medtimer.statistics.model.ReminderTableRowData
import com.futsch1.medtimer.statistics.model.TakenSkippedData
import com.futsch1.medtimer.statistics.ui.calendar.CalendarDayEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

interface StatisticsScreenState {
    val medicinePerDayData: MedicinePerDayData?
    val takenSkippedData: TakenSkippedData?
    val takenSkippedTotalData: TakenSkippedData?
    val filterText: String
    val tableRows: ImmutableList<ReminderTableRowData>
    val selectedTab: StatisticsTabType
    val selectedDays: AnalysisDays
    val dayEvents: ImmutableMap<LocalDate, List<CalendarDayEvent>>
}

private class MutableStatisticsScreenState : StatisticsScreenState {
    override var medicinePerDayData: MedicinePerDayData? by mutableStateOf(null)
    override var takenSkippedData: TakenSkippedData? by mutableStateOf(null)
    override var takenSkippedTotalData: TakenSkippedData? by mutableStateOf(null)
    override var filterText: String by mutableStateOf("")
    override var selectedTab: StatisticsTabType by mutableStateOf(StatisticsTabType.CHARTS)
    override var selectedDays: AnalysisDays by mutableStateOf(AnalysisDays.DEFAULT)
    override var dayEvents: ImmutableMap<LocalDate, List<CalendarDayEvent>> by mutableStateOf(persistentMapOf())
    var debouncedFilterText: String by mutableStateOf("")
    var allTableRows: ImmutableList<ReminderTableRowData> by mutableStateOf(persistentListOf())
    override val tableRows: ImmutableList<ReminderTableRowData> by derivedStateOf {
        if (debouncedFilterText.isBlank()) {
            allTableRows
        } else {
            val lower = debouncedFilterText.lowercase()
            allTableRows.filter { row ->
                row.medicineName.lowercase().contains(lower) ||
                        row.dosage.lowercase().contains(lower)
            }.toImmutableList()
        }
    }
}

@HiltViewModel
class StatisticsScreenViewModel @Inject constructor(
    private val repository: MedicineRepository,
    private val analysisDaysPreference: AnalysisDaysPreference,
    private val statisticsTabPreference: StatisticsTabPreference,
    private val getCalendarEvents: GetCalendarEventsUseCase,
) : ViewModel() {
    private val _state = MutableStatisticsScreenState()
    val state: StatisticsScreenState get() = _state

    private var chartLoadJob: Job? = null
    private val searchQueryFlow = MutableStateFlow("")

    init {
        _state.selectedTab = statisticsTabPreference.activeFragment
        _state.selectedDays = analysisDaysPreference.analysisDays

        searchQueryFlow
            .onEach { _state.filterText = it }
            .launchIn(viewModelScope)

        @Suppress("OPT_IN_USAGE")
        searchQueryFlow
            .debounce(300)
            .onEach { _state.debouncedFilterText = it }
            .launchIn(viewModelScope)

        val zone = ZoneId.systemDefault()
        repository.getReminderEventsFlow(0, statusValuesWithoutDeletedAndAcknowledged)
            .onEach { events -> loadTableData(events, zone) }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            _state.dayEvents = getCalendarEvents(medicineId = -1, pastMonths = 3, futureMonths = 0)
        }
    }

    fun updateFilterText(query: String) {
        searchQueryFlow.value = query
    }

    fun selectTab(tab: StatisticsTabType) {
        _state.selectedTab = tab
        statisticsTabPreference.activeFragment = tab
    }

    fun selectDays(days: AnalysisDays) {
        _state.selectedDays = days
        analysisDaysPreference.analysisDays = days
    }

    companion object {
        val FALLBACK_COLORS = listOf(
            Color(0xFF003f5c), Color(0xFF2f4b7c), Color(0xFF665191), Color(0xFFa05195),
            Color(0xFFd45087), Color(0xFFf95d6a), Color(0xFFff7c43), Color(0xFFffa600),
            Color(0xFF004c6d), Color(0xFF295d7d), Color(0xFF436f8e), Color(0xFF5b829f),
            Color(0xFF7295b0), Color(0xFF89a8c2), Color(0xFFa1bcd4), Color(0xFFb8d0e6),
            Color(0xFFd0e5f8),
        )
    }

    fun loadChartData(days: Int, periodTitle: String, totalTitle: String) {
        chartLoadJob?.cancel()
        chartLoadJob = viewModelScope.launch(Dispatchers.IO) {
            val statisticsProvider = StatisticsProvider(repository)

            val seriesList = statisticsProvider.getLastDaysReminders(days)
            val medicines = repository.medicines
            _state.medicinePerDayData = buildMedicinePerDayData(seriesList, medicines)

            val data = statisticsProvider.getTakenSkippedData(days)
            _state.takenSkippedData = TakenSkippedData(data.taken, data.skipped, periodTitle)

            val dataTotal = statisticsProvider.getTakenSkippedData(0)
            _state.takenSkippedTotalData =
                TakenSkippedData(dataTotal.taken, dataTotal.skipped, totalTitle)
        }
    }

    suspend fun getReminderEvent(eventId: Int): ReminderEvent? {
        return withContext(Dispatchers.IO) {
            repository.getReminderEvent(eventId)
        }
    }

    private fun loadTableData(
        events: List<ReminderEvent>,
        zone: ZoneId,
    ) {
        val rows = events.map { event ->
            ReminderTableRowData(
                eventId = event.reminderEventId,
                takenAt = if (event.status == ReminderEvent.ReminderStatus.TAKEN)
                    Instant.ofEpochSecond(event.processedTimestamp).atZone(zone).toLocalDateTime()
                else null,
                takenStatus = event.status,
                medicineName = event.medicineName,
                dosage = event.amount,
                remindedAt = Instant.ofEpochSecond(event.remindedTimestamp).atZone(zone)
                    .toLocalDateTime(),
            )
        }.toImmutableList()
        _state.allTableRows = rows
    }

    private fun buildMedicinePerDayData(
        seriesList: List<MedicinePerDaySeries>,
        medicines: List<FullMedicine>,
    ): MedicinePerDayData {
        val dates = seriesList.firstOrNull()?.xValues
            ?.map { LocalDate.ofEpochDay(it) }?.toImmutableList() ?: persistentListOf()

        val medicineMap = medicines.associateBy { it.medicine.name }

        val series = seriesList.mapIndexed { index, s ->
            val fm = medicineMap[s.name]
            val resolvedColor = if (fm != null && fm.medicine.useColor) {
                Color(fm.medicine.color)
            } else {
                FALLBACK_COLORS[index % FALLBACK_COLORS.size]
            }
            MedicineSeriesData(s.name, s.yValues.toImmutableList(), resolvedColor)
        }.toImmutableList()

        return MedicinePerDayData("", dates, series)
    }
}