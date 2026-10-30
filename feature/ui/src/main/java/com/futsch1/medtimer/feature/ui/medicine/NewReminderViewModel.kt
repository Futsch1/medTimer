package com.futsch1.medtimer.feature.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderTime
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.Interval
import com.futsch1.medtimer.core.ui.TimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class NewReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    timeFormatter: TimeFormatter,
) : ViewModel() {
    private val mutableState = MutableNewReminderState(timeFormatter)
    val state: NewReminderState = mutableState

    private var medicineId: Int = 0

    fun startNewReminder(
        medicineId: Int,
        amount: Double
    ) {
        this.medicineId = medicineId
        val defaultTime = LocalTime.of(ReminderTime.DEFAULT_TIME / 60, ReminderTime.DEFAULT_TIME % 60)
        val defaultDateTime = LocalDateTime.of(LocalDate.now(), defaultTime)
        mutableState.draft = NewReminderDraft(
            time = defaultTime,
            intervalStart = defaultDateTime,
            threshold = amount.toInt().toString()
        )
        mutableState.created = false
        mutableState.invalid = false
    }

    fun updateDraft(draft: NewReminderDraft) {
        mutableState.draft = draft
        mutableState.invalid = false
    }

    fun createReminder(): Boolean {
        val reminder = mutableState.draft.toReminder(medicineId)
        if (reminder == null) {
            mutableState.invalid = true
            return false
        }

        viewModelScope.launch {
            reminderRepository.create(reminder)
            mutableState.created = true
        }
        return true
    }
}

internal fun NewReminderDraft.toReminder(medicineId: Int): Reminder? = runCatching {
    val type = checkNotNull(selectedType)
    val timeMinutes = time.hour * 60 + time.minute
    val base = Reminder.default().copy(
        medicineRelId = medicineId,
        createdTime = Instant.now(),
        cycleStartDay = LocalDate.now().plusDays(1),
        instructions = "",
        time = ReminderTime(timeMinutes)
    )

    when (type) {
        ReminderType.TIME_BASED -> base.copy(amount = amount.trim())

        ReminderType.CONTINUOUS_INTERVAL -> base.copy(
            amount = amount.trim(),
            time = ReminderTime(intervalMinutes(Interval.MAX_INTERVAL_MINUTES), isDuration = true),
            intervalStart = intervalStart.atZone(ZoneId.systemDefault()).toInstant(),
            intervalStartsFromProcessed = intervalStartsFromProcessed,
        )

        ReminderType.WINDOWED_INTERVAL -> base.copy(
            amount = amount.trim(),
            time = ReminderTime(intervalMinutes(24 * 60), isDuration = true),
            windowedInterval = true,
            intervalStartTimeOfDay = dailyStart,
            intervalEndTimeOfDay = dailyEnd,
        )

        ReminderType.OUT_OF_STOCK -> base.copy(
            outOfStockThreshold = checkNotNull(MedicineHelper.parseAmount(threshold)),
            outOfStockReminderType = when (stockReminderType) {
                NewReminderStockReminderType.ONCE -> Reminder.OutOfStockReminderType.ONCE
                NewReminderStockReminderType.ALWAYS -> Reminder.OutOfStockReminderType.ALWAYS
                NewReminderStockReminderType.DAILY -> Reminder.OutOfStockReminderType.DAILY
            },
        )

        ReminderType.EXPIRATION_DATE -> base.copy(
            expirationReminderType = when (expirationReminderType) {
                NewReminderExpirationReminderType.ONCE -> Reminder.ExpirationReminderType.ONCE
                NewReminderExpirationReminderType.DAILY -> Reminder.ExpirationReminderType.DAILY
            },
            periodStart = LocalDate.ofEpochDay(expirationDays.toLong()),
        )

        ReminderType.LINKED, ReminderType.REFILL -> error("Unsupported new reminder type: $type")
    }
}.getOrNull()

private fun NewReminderDraft.intervalMinutes(maxMinutes: Int): Int {
    val value = interval.toIntOrNull()?.coerceAtLeast(1) ?: 12
    return Interval(value, intervalUnit).minutesValue.coerceAtMost(maxMinutes)
}

