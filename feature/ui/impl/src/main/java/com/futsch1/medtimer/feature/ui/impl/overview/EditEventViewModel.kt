package com.futsch1.medtimer.feature.ui.impl.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class EditEventViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reminderEventRepository: ReminderEventRepository,
    private val timeFormatter: TimeFormatter,
) : ViewModel() {

    companion object {
        const val ARG_REMINDER_EVENT_ID = "reminder_event_id"
    }

    private val reminderEventId: Int = checkNotNull(savedStateHandle[ARG_REMINDER_EVENT_ID])
    private val zoneId = ZoneId.systemDefault()
    private var storedEvent: ReminderEvent? = null

    private val _medicineName = MutableStateFlow("")
    val medicineName: StateFlow<String> = _medicineName.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    fun setMedicineName(value: String) { _medicineName.value = value }
    fun setAmount(value: String) { _amount.value = value }
    fun setNotes(value: String) { _notes.value = value }

    var status: ReminderEvent.ReminderStatus? = null

    private val _reminderStatus = MutableStateFlow<ReminderEvent.ReminderStatus?>(null)
    val reminderStatus: StateFlow<ReminderEvent.ReminderStatus?> = _reminderStatus.asStateFlow()

    private val _remindedMinutes = MutableStateFlow(0)
    var remindedMinutes: Int
        get() = _remindedMinutes.value
        set(value) {
            _remindedMinutes.value = value
        }
    val remindedTimeString: StateFlow<String> = _remindedMinutes
        .map { timeFormatter.minutesToTimeString(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _remindedDate = MutableStateFlow(LocalDate.now())
    var remindedDate: LocalDate
        get() = _remindedDate.value
        set(value) {
            _remindedDate.value = value
        }
    val remindedDateString: StateFlow<String> = _remindedDate
        .map { timeFormatter.localDateToString(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _processedMinutes = MutableStateFlow(0)
    var processedMinutes: Int
        get() = _processedMinutes.value
        set(value) {
            _processedMinutes.value = value
        }
    val processedTimeString: StateFlow<String> = _processedMinutes
        .map { timeFormatter.minutesToTimeString(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _processedDate = MutableStateFlow(LocalDate.now())
    var processedDate: LocalDate
        get() = _processedDate.value
        set(value) {
            _processedDate.value = value
        }
    val processedDateString: StateFlow<String> = _processedDate
        .map { timeFormatter.localDateToString(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        viewModelScope.launch {
            reminderEventRepository.getFlow(reminderEventId)
                .filterNotNull()
                .first()
                .let { event ->
                    storedEvent = event
                    _medicineName.value = event.medicineName
                    _amount.value = event.amount
                    _notes.value = event.notes
                    status = when (event.status) {
                        ReminderEvent.ReminderStatus.TAKEN -> ReminderEvent.ReminderStatus.TAKEN
                        ReminderEvent.ReminderStatus.SKIPPED, ReminderEvent.ReminderStatus.RAISED -> ReminderEvent.ReminderStatus.SKIPPED
                        else -> null
                    }
                    _remindedMinutes.value = timestampToMinutes(event.remindedTimestamp)
                    _remindedDate.value = TimeHelper.secondsSinceEpochToLocalDate(event.remindedTimestamp.epochSecond, zoneId)
                    _processedMinutes.value = timestampToMinutes(event.processedTimestamp)
                    _processedDate.value = TimeHelper.secondsSinceEpochToLocalDate(event.processedTimestamp.epochSecond, zoneId)
                    _reminderStatus.value = event.status
                }
        }
    }

    fun updateEvent() {
        viewModelScope.launch {
            val event = storedEvent ?: return@launch
            val remindedTimestamp = computeTimestamp(event.remindedTimestamp, _remindedMinutes.value, _remindedDate.value)
            val processedTimestamp = computeTimestamp(event.processedTimestamp, _processedMinutes.value, _processedDate.value)

            val updatedEvent = event.copy(
                medicineName = medicineName.value,
                amount = amount.value,
                notes = notes.value,
                remindedTimestamp = remindedTimestamp,
                processedTimestamp = processedTimestamp,
                status = status ?: event.status
            )

            reminderEventRepository.update(updatedEvent)
        }
    }

    private fun computeTimestamp(original: Instant, minutes: Int, date: LocalDate): Instant {
        val withTime = TimeHelper.changeTimeMinutes(original, minutes)
        return TimeHelper.changeInstantDate(withTime, date)
    }

    private fun timestampToMinutes(timestamp: Instant): Int {
        val localTime = TimeHelper.secondsSinceEpochToLocalTime(timestamp.epochSecond, zoneId)
        return localTime.hour * 60 + localTime.minute
    }
}
