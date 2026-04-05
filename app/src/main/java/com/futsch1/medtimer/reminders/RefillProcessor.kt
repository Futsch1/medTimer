package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.database.TagEntity
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ReminderType
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import java.util.stream.Collectors
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
        val medicine = medicineRepository.getFull(medicineId) ?: return

        val refillEvent = processRefillInternal(medicine)
        medicineRepository.update(medicine.medicine)
        reminderEventRepository.create(refillEvent)

        if (reminderEvent != null) {
            notificationProcessor.processReminderEventsInNotification(
                ProcessedNotificationData.fromReminderEvents(listOf(reminderEvent)),
                ReminderEvent.ReminderStatus.ACKNOWLEDGED
            )
        }
    }

    private fun processRefillInternal(medicine: FullMedicineEntity): ReminderEvent {
        medicine.medicine.amount += medicine.medicine.refillSize
        Log.d(LogTags.STOCK_HANDLING, "Refill medicine ${medicine.medicine.name} with ${medicine.medicine.refillSize} to amount ${medicine.medicine.amount}")

        return buildReminderEvent(medicine)
    }

    fun buildReminderEvent(medicine: FullMedicineEntity): ReminderEvent {
        val reminderEvent = ReminderEvent.default().copy(
            medicineName = medicine.medicine.name,
            color = medicine.medicine.color,
            useColor = medicine.medicine.useColor,
            iconId = medicine.medicine.iconId,
            tags = medicine.tags.stream().map { t: TagEntity? -> t!!.name }.collect((Collectors.toList())),
            amount = "${MedicineHelper.formatAmount(medicine.medicine.amount - medicine.medicine.refillSize, medicine.medicine.unit)} ➡ ${
                MedicineHelper.formatAmount(
                    medicine.medicine.amount,
                    medicine.medicine.unit
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
