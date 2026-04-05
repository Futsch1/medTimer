package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ReminderType
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import javax.inject.Inject

class RefillProcessor @Inject constructor(
    private val notificationProcessor: NotificationProcessor,
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val timeAccess: TimeAccess
) {
    suspend fun processRefill(processedNotificationData: ProcessedNotificationData) {
        val reminderEvent = reminderEventRepository.get(processedNotificationData.reminderEventIds[0])
        val reminder = reminderEvent?.let { reminderRepository.get(it.reminderId) }
        reminder?.let { processRefill(it.medicineRelId, reminderEvent) }

    }

    suspend fun processRefill(medicineId: Int?, reminderEvent: ReminderEvent? = null) {
        if (medicineId == null) {
            return
        }
        val medicine = medicineRepository.get(medicineId) ?: return

        val refillEvent = processRefillInternal(medicine, medicineRepository)
        reminderEventRepository.create(refillEvent)

        if (reminderEvent != null) {
            notificationProcessor.processReminderEventsInNotification(
                ProcessedNotificationData.fromReminderEvents(listOf(reminderEvent)),
                ReminderEvent.ReminderStatus.ACKNOWLEDGED
            )
        }
    }

    private suspend fun processRefillInternal(medicine: Medicine, medicineRepository: MedicineRepository): ReminderEvent {
        medicineRepository.update(medicine.copy(amount = medicine.amount + medicine.refillSize))
        Log.d(LogTags.STOCK_HANDLING, "Refill medicine ${medicine.name} with ${medicine.refillSize} to amount ${medicine.amount + medicine.refillSize}")

        return buildReminderEvent(medicine)
    }

    fun buildReminderEvent(medicine: Medicine): ReminderEvent {
        val reminderEvent = ReminderEvent.default().copy(
            medicineName = medicine.name,
            color = medicine.color,
            useColor = medicine.useColor,
            iconId = medicine.iconId,
            tags = medicine.tags.map { it.name },
            amount = "${MedicineHelper.formatAmount(medicine.amount - medicine.refillSize, medicine.unit)} ➡ ${
                MedicineHelper.formatAmount(
                    medicine.amount,
                    medicine.unit
                )
            }",
            status = ReminderEvent.ReminderStatus.ACKNOWLEDGED,
            remindedTimestamp = timeAccess.now(),
            processedTimestamp = timeAccess.now(),
            reminderId = -1,
            reminderType = ReminderType.REFILL
        )

        return reminderEvent
    }
}
