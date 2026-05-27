package com.futsch1.medtimer.feature.reminders

import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.api.scheduling.CyclesHelper
import java.time.Instant

suspend fun buildReminderEvent(
    remindedTimeStamp: Long,
    medicine: Medicine,
    reminder: Reminder,
    reminderEventRepository: ReminderEventRepository,
    timeFormatter: TimeFormatter
): ReminderEvent {
    val remindedInstant = Instant.ofEpochSecond(remindedTimeStamp)
    val amount = when (reminder.reminderType) {
        ReminderType.OUT_OF_STOCK -> {
            MedicineHelper.formatAmount(medicine.amount, medicine.unit)
        }

        ReminderType.EXPIRATION_DATE -> {
            timeFormatter.localDateToString(medicine.expirationDate)
        }

        else -> {
            reminder.amount
        }
    }

    val lastIntervalReminderTimeInMinutes = if (reminder.isInterval) {
        getLastReminderEventTimeInMinutes(
            reminderEventRepository,
            reminder.id,
            remindedInstant,
            reminder.reminderType == ReminderType.WINDOWED_INTERVAL
        )
    } else {
        0
    }

    return ReminderEvent(
        reminderEventId = 0,
        reminderId = reminder.id,
        medicineName = medicine.name + CyclesHelper.getCycleCountString(reminder),
        amount = amount,
        color = medicine.color,
        useColor = medicine.useColor,
        status = ReminderEvent.ReminderStatus.RAISED,
        remindedTimestamp = remindedInstant,
        processedTimestamp = Instant.EPOCH,
        notificationId = 0,
        iconId = medicine.iconId,
        remainingRepeats = 0,
        notes = "",
        reminderType = reminder.reminderType,
        stockHandled = false,
        askForAmount = reminder.variableAmount,
        tags = medicine.tags.map { it.name },
        lastIntervalReminderTimeInMinutes = lastIntervalReminderTimeInMinutes,
        stockBefore = if (medicine.isStockManagementActive()) medicine.amount else -1.0,
        stockAfter = if (medicine.isStockManagementActive()) medicine.amount else -1.0,
        stockUnit = medicine.unit
    )
}

private suspend fun getLastReminderEventTimeInMinutes(
    reminderEventRepository: ReminderEventRepository,
    reminderId: Int,
    remindedTimestamp: Instant,
    isWindowedInterval: Boolean
): Int {
    val lastReminderEvent = reminderEventRepository.getLast(reminderId) ?: return 0
    if (lastReminderEvent.status != ReminderEvent.ReminderStatus.TAKEN) return 0

    if (isWindowedInterval &&
        TimeHelper.isSameDay(lastReminderEvent.remindedTimestamp, remindedTimestamp)
    ) {
        return 0
    }

    return (lastReminderEvent.processedTimestamp.epochSecond / 60).toInt()
}
