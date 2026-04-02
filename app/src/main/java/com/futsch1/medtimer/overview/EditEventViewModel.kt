package com.futsch1.medtimer.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.helpers.TimeHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class EditEventViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val medicineRepository: MedicineRepository,
    private val timeFormatter: TimeFormatter,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    companion object {
        const val ARG_REMINDER_EVENT_ID = "reminder_event_id"
    }

    private val reminderEventId: Int = checkNotNull(savedStateHandle[ARG_REMINDER_EVENT_ID])
    private val zoneId = ZoneId.systemDefault()
    private var storedEvent: ReminderEventEntity? = null

    val medicineName = MutableStateFlow("")
    val amount = MutableStateFlow("")
    val notes = MutableStateFlow("")

    var status: ReminderEventEntity.ReminderStatus? = null

    private val _reminderStatus = MutableStateFlow<ReminderEventEntity.ReminderStatus?>(null)
    val reminderStatus: StateFlow<ReminderEventEntity.ReminderStatus?> = _reminderStatus.asStateFlow()

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
            medicineRepository.getReminderEventFlow(reminderEventId)
                .filterNotNull()
                .first()
                .let { event ->
                    storedEvent = event
                    medicineName.value = event.medicineName
                    amount.value = event.amount
                    notes.value = event.notes
                    status = when (event.status) {
                        ReminderEventEntity.ReminderStatus.TAKEN -> ReminderEventEntity.ReminderStatus.TAKEN
                        ReminderEventEntity.ReminderStatus.SKIPPED, ReminderEventEntity.ReminderStatus.RAISED -> ReminderEventEntity.ReminderStatus.SKIPPED
                        else -> null
                    }
                    _remindedMinutes.value = timestampToMinutes(event.remindedTimestamp)
                    _remindedDate.value = TimeHelper.secondsSinceEpochToLocalDate(event.remindedTimestamp, zoneId)
                    _processedMinutes.value = timestampToMinutes(event.processedTimestamp)
                    _processedDate.value = TimeHelper.secondsSinceEpochToLocalDate(event.processedTimestamp, zoneId)
                    _reminderStatus.value = event.status
                }
        }
    }

    suspend fun updateEvent() {
        val event = storedEvent ?: return
        event.medicineName = medicineName.value
        event.amount = amount.value
        event.notes = notes.value
        event.remindedTimestamp = computeTimestamp(event.remindedTimestamp, _remindedMinutes.value, _remindedDate.value)
        event.processedTimestamp = computeTimestamp(event.processedTimestamp, _processedMinutes.value, _processedDate.value)
        status?.let { event.status = it }

        withContext(ioDispatcher) {
            medicineRepository.updateReminderEvent(event)
        }
    }

    private fun computeTimestamp(original: Long, minutes: Int, date: LocalDate): Long {
        val withTime = TimeHelper.changeTimeStampMinutes(original, minutes)
        return TimeHelper.changeTimeStampDate(withTime, date)
    }

    private fun timestampToMinutes(timestamp: Long): Int {
        val localTime = TimeHelper.secondsSinceEpochToLocalTime(timestamp, zoneId)
        return localTime.hour * 60 + localTime.minute
    }
}
