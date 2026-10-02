package com.futsch1.medtimer.feature.ui.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
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
    private val reminderRepository: ReminderRepository,
    private val commandBus: ReminderCommandBus,
    private val timeFormatter: TimeFormatter,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
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

    fun setMedicineName(value: String) {
        _medicineName.value = value
    }

    fun setAmount(value: String) {
        _amount.value = value
    }

    fun setNotes(value: String) {
        _notes.value = value
    }

    var status: ReminderEvent.ReminderStatus? = null

    private val _reminderStatus = MutableStateFlow<Pair<ReminderEvent.ReminderStatus?, Boolean>>(null to false)
    val reminderStatus: StateFlow<Pair<ReminderEvent.ReminderStatus?, Boolean>> = _reminderStatus.asStateFlow()

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
                    _remindedDate.value =
                        TimeHelper.secondsSinceEpochToLocalDate(event.remindedTimestamp.epochSecond, zoneId)
                    _processedMinutes.value = timestampToMinutes(event.processedTimestamp)
                    _processedDate.value =
                        TimeHelper.secondsSinceEpochToLocalDate(event.processedTimestamp.epochSecond, zoneId)
                    _reminderStatus.value = event.status to event.cannotBeSkipped
                }
        }
    }

    /**
     * Called when the sheet is dismissed, right before it is removed and this ViewModel is cleared,
     * so the save runs in the application scope to complete regardless.
     */
    fun updateEvent() {
        applicationScope.launch {
            val event = storedEvent ?: return@launch
            val remindedTimestamp =
                computeTimestamp(event.remindedTimestamp, _remindedMinutes.value, _remindedDate.value)
            val processedTimestamp =
                computeTimestamp(event.processedTimestamp, _processedMinutes.value, _processedDate.value)

            val updatedEvent = event.copy(
                medicineName = medicineName.value,
                amount = amount.value,
                notes = notes.value,
                remindedTimestamp = remindedTimestamp,
                processedTimestamp = processedTimestamp,
                status = status ?: event.status
            )

            reminderEventRepository.update(correctStock(event, updatedEvent))
        }
    }

    /**
     * Keeps the medicine's stock in line with an edited amount or taken/skipped status,
     * deducting or returning the difference to what was deducted when the event was processed.
     * Manual doses carry no reminder and therefore no link to a medicine, so they are left as they are.
     */
    private suspend fun correctStock(original: ReminderEvent, edited: ReminderEvent): ReminderEvent {
        val medicineId = reminderRepository.fetch(original.reminderId)?.medicineRelId ?: return edited
        val deducted = if (original.stockHandled) MedicineHelper.parseAmount(original.amount) ?: 0.0 else 0.0
        val toDeduct = if (edited.status == ReminderEvent.ReminderStatus.TAKEN) MedicineHelper.parseAmount(edited.amount) else null
        val correction = (toDeduct ?: 0.0) - deducted
        val corrected = edited.copy(stockHandled = toDeduct != null)
        if (correction == 0.0) {
            return corrected
        }

        commandBus.processStockHandling(correction, medicineId, edited.processedTimestamp.epochSecond)
        return if (corrected.stockAfter >= 0) corrected.copy(stockAfter = maxOf(0.0, corrected.stockAfter - correction)) else corrected
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
