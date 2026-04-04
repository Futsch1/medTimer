package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.database.TagEntity
import com.futsch1.medtimer.database.toEntity
import com.futsch1.medtimer.database.toModel
import com.futsch1.medtimer.helpers.MedicineHelper
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
        val reminderEvent = reminderEventRepository.get(processedNotificationData.reminderEventIds[0])!!.toEntity()
        val reminder = reminderRepository.get(reminderEvent.reminderId)
        val medicineId = reminder!!.medicineRelId
        processRefill(medicineId, reminderEvent)
    }

    suspend fun processRefill(medicineId: Int, reminderEvent: ReminderEventEntity? = null) {
        val medicine = medicineRepository.getFull(medicineId)!!

        val refillEvent = processRefillInternal(medicine)
        medicineRepository.update(medicine.medicine)
        reminderEventRepository.create(refillEvent.toModel())

        if (reminderEvent != null) {
            notificationProcessor.processReminderEventsInNotification(
                ProcessedNotificationData.fromReminderEvents(listOf(reminderEvent)),
                ReminderEventEntity.ReminderStatus.ACKNOWLEDGED
            )
        }
    }

    private fun processRefillInternal(medicine: FullMedicineEntity): ReminderEventEntity {
        medicine.medicine.amount += medicine.medicine.refillSize
        Log.d(LogTags.STOCK_HANDLING, "Refill medicine ${medicine.medicine.name} with ${medicine.medicine.refillSize} to amount ${medicine.medicine.amount}")

        return buildReminderEvent(medicine)
    }

    fun buildReminderEvent(medicine: FullMedicineEntity): ReminderEventEntity {
        val reminderEvent = ReminderEventEntity()
        reminderEvent.reminderId = -1
        reminderEvent.remindedTimestamp = timeAccess.now().toEpochMilli() / 1000
        reminderEvent.processedTimestamp = reminderEvent.remindedTimestamp
        reminderEvent.medicineName = medicine.medicine.name
        reminderEvent.color = medicine.medicine.color
        reminderEvent.useColor = medicine.medicine.useColor
        reminderEvent.status = ReminderEventEntity.ReminderStatus.ACKNOWLEDGED
        reminderEvent.iconId = medicine.medicine.iconId
        reminderEvent.askForAmount = false
        reminderEvent.tags = medicine.tags.stream().map { t: TagEntity? -> t!!.name }.collect((Collectors.toList()))
        reminderEvent.reminderType = ReminderEntity.ReminderType.REFILL

        reminderEvent.amount = "${MedicineHelper.formatAmount(medicine.medicine.amount - medicine.medicine.refillSize, medicine.medicine.unit)} ➡ ${
            MedicineHelper.formatAmount(
                medicine.medicine.amount,
                medicine.medicine.unit
            )
        }"

        return reminderEvent
    }
}
