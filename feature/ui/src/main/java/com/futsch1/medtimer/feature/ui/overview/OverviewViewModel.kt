package com.futsch1.medtimer.feature.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.model.Tag
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.TagRepository
import com.futsch1.medtimer.feature.reminders.FutureRemindersRepository
import com.futsch1.medtimer.feature.ui.overview.model.EventPosition
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import com.futsch1.medtimer.feature.ui.overview.model.ScheduledReminderEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

@HiltViewModel(assistedFactory = OverviewViewModel.Factory::class)
class OverviewViewModel @AssistedInject constructor(
    preferencesDataSource: PreferencesDataSource,
    private val reminderEventRepository: ReminderEventRepository,
    tagRepository: TagRepository,
    private val futureRemindersRepository: FutureRemindersRepository,
    private val reminderEventFactory: PastReminderEvent.Factory,
    private val scheduledReminderEventFactory: ScheduledReminderEvent.Factory,
    @Assisted private val validTagIds: StateFlow<Set<Int>?>,
    @Assisted private val scheduledReminders: SharedFlow<List<ScheduledReminder>>
) : ViewModel() {
    private data class FilterState(
        val activeFilters: Set<OverviewFilter>,
        val day: LocalDate,
        val tick: Long
    )

    @AssistedFactory
    fun interface Factory {
        fun create(
            validTagIds: StateFlow<Set<Int>?>,
            scheduledReminders: SharedFlow<List<ScheduledReminder>>
        ): OverviewViewModel
    }

    private var _initialized = false
    val initialized get() = _initialized

    private val filterState = MutableStateFlow(FilterState(emptySet(), LocalDate.now(), 0L))

    // Expands from default 6-day window to Instant.EPOCH when user scrolls into past
    private val queryStart = MutableStateFlow(Instant.now().minus(Duration.of(6, ChronoUnit.DAYS)))

    private val liveTags: StateFlow<List<Tag>> = tagRepository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val simulatedThrough: StateFlow<LocalDate> = futureRemindersRepository.simulatedThrough

    var day: LocalDate
        get() = filterState.value.day
        set(value) {
            filterState.update { it.copy(day = value) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val reminderEvents: Flow<List<ReminderEvent>> = queryStart.flatMapLatest { start ->
        combine(
            reminderEventRepository.getAllFlow(start, ReminderEvent.statusValuesWithoutDelete),
            validTagIds,
            liveTags
        ) { events, tagIds, tags ->
            filterByTags(events, tagIds, tags)
        }
    }

    val overviewEvents: SharedFlow<List<OverviewEvent>> =
        combine(reminderEvents, scheduledReminders, filterState) { events, reminders, fs ->
            getFiltered(events, reminders, fs)
        }.onEach { _initialized = true }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        viewModelScope.launch {
            filterState.collect { fs ->
                // Expand past query when user scrolls beyond the default 6-day window
                if (fs.day < LocalDate.now().minusDays(6) && queryStart.value > Instant.EPOCH) {
                    queryStart.value = Instant.EPOCH
                }
                // Request a wider simulation window when scrolling far into the future
                val dayOffset = ChronoUnit.DAYS.between(LocalDate.now(), fs.day)
                if (dayOffset >= 21) {
                    futureRemindersRepository.requestWindow("overview", dayOffset + 28)
                } else {
                    futureRemindersRepository.releaseWindow("overview")
                }
            }
        }

        if (preferencesDataSource.preferences.value.useRelativeDateTime) {
            viewModelScope.launch {
                while (true) {
                    delay(60.seconds)
                    update()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        futureRemindersRepository.releaseWindow("overview")
    }

    fun update() {
        filterState.update { it.copy(tick = it.tick + 1) }
    }

    fun addFilter(f: OverviewFilter) {
        filterState.update { it.copy(activeFilters = it.activeFilters + f) }
    }

    fun removeFilter(f: OverviewFilter) {
        filterState.update { it.copy(activeFilters = it.activeFilters - f) }
    }

    fun setFilters(filters: Set<OverviewFilter>) {
        filterState.update { it.copy(activeFilters = filters) }
    }

    private fun filterByTags(
        events: List<ReminderEvent>,
        tagIds: Set<Int>?,
        tags: List<Tag>
    ): List<ReminderEvent> {
        if (tagIds.isNullOrEmpty()) return events
        val validTagNames = tags.filter { tagIds.contains(it.id) }.map { it.name }.toSet()
        return events.filter { event -> event.tags.any { it in validTagNames } }
    }

    private fun getFiltered(
        events: List<ReminderEvent>,
        reminders: List<ScheduledReminder>,
        filterState: FilterState
    ): List<OverviewEvent> {
        val filteredOverviewEvents = mutableListOf<OverviewEvent>()

        for (reminderEvent in events) {
            if (isReminderEventVisible(reminderEvent, filterState)) {
                filteredOverviewEvents.add(reminderEventFactory.create(reminderEvent))
            }
        }

        for (scheduledReminder in reminders) {
            if (isScheduledReminderVisible(scheduledReminder, filterState)) {
                filteredOverviewEvents.add(scheduledReminderEventFactory.create(scheduledReminder))
            }
        }

        return assignPositions(filteredOverviewEvents.sortedWith(compareBy<OverviewEvent> { it.timestamp }.thenBy { it.id }))
    }

    private fun assignPositions(overviewEvents: List<OverviewEvent>): List<OverviewEvent> {
        overviewEvents.forEach { overviewEvent ->
            overviewEvent.eventPosition = EventPosition.MIDDLE
        }
        if (overviewEvents.size == 1) {
            overviewEvents[0].eventPosition = EventPosition.ONLY
        } else {
            overviewEvents.firstOrNull()?.eventPosition = EventPosition.FIRST
            overviewEvents.lastOrNull()?.eventPosition = EventPosition.LAST
        }
        return overviewEvents
    }

    private fun isScheduledReminderVisible(
        scheduledReminder: ScheduledReminder,
        filterState: FilterState
    ): Boolean {
        val scheduledRemindersVisible =
            filterState.activeFilters.isEmpty() || filterState.activeFilters.contains(OverviewFilter.SCHEDULED)
        return TimeHelper.isOnDay(
            scheduledReminder.timestamp.epochSecond,
            filterState.day.toEpochDay(),
            ZoneId.systemDefault()
        ) && scheduledRemindersVisible
    }

    private fun isReminderEventVisible(
        reminderEvent: ReminderEvent,
        filterState: FilterState
    ): Boolean {
        val reminderEventVisible = filterState.activeFilters.isEmpty() ||
                ((reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN || reminderEvent.status == ReminderEvent.ReminderStatus.ACKNOWLEDGED) && filterState.activeFilters.contains(
                    OverviewFilter.TAKEN
                )) ||
                (reminderEvent.status == ReminderEvent.ReminderStatus.SKIPPED && filterState.activeFilters.contains(
                    OverviewFilter.SKIPPED
                )) ||
                (reminderEvent.status == ReminderEvent.ReminderStatus.RAISED && filterState.activeFilters.contains(
                    OverviewFilter.RAISED
                ))
        return TimeHelper.isOnDay(
            reminderEvent.remindedTimestamp.epochSecond,
            filterState.day.toEpochDay(),
            ZoneId.systemDefault()
        ) && reminderEventVisible
    }
}
