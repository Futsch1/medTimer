package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import java.time.Instant
import java.util.stream.Collectors

class RefillProcessor(val reminderContext: ReminderContext) {
    private val medicineRepository = reminderContext.medicineRepository

    fun processRefill(processedNotificationData: ProcessedNotificationData) {
        val reminderEvent = medicineRepository.getReminderEvent(processedNotificationData.reminderEventIds[0])!!
        val reminder = medicineRepository.getReminder(reminderEvent.reminderId)
        val medicineId = reminder!!.medicineRelId
        processRefill(medicineId, reminderEvent)
    }

    fun processRefill(medicineId: Int, reminderEvent: ReminderEvent? = null) {
        val medicine = medicineRepository.getMedicine(medicineId)!!

        val refillEvent = processRefillInternal(medicine)
        medicineRepository.updateMedicine(medicine.medicine)
        medicineRepository.insertReminderEvent(refillEvent)

        if (reminderEvent != null) {
            NotificationProcessor(reminderContext).processReminderEventsInNotification(
                ProcessedNotificationData.fromReminderEvents(listOf(reminderEvent)),
                ReminderEvent.ReminderStatus.ACKNOWLEDGED
            )
        }
    }

    private fun processRefillInternal(medicine: FullMedicine): ReminderEvent {
        medicine.medicine.amount += medicine.medicine.refillSize
        Log.d(LogTags.STOCK_HANDLING, "Refill medicine ${medicine.medicine.name} with ${medicine.medicine.refillSize} to amount ${medicine.medicine.amount}")

        // Create refill reminder event
        return buildReminderEvent(medicine)
    }

    fun buildReminderEvent(medicine: FullMedicine): ReminderEvent {
        val reminderEvent = ReminderEvent()
        reminderEvent.reminderId = -1
        reminderEvent.remindedTimestamp = Instant.now().toEpochMilli() / 1000
        reminderEvent.processedTimestamp = reminderEvent.remindedTimestamp
        reminderEvent.medicineName = medicine.medicine.name
        reminderEvent.color = medicine.medicine.color
        reminderEvent.useColor = medicine.medicine.useColor
        reminderEvent.status = ReminderEvent.ReminderStatus.ACKNOWLEDGED
        reminderEvent.iconId = medicine.medicine.iconId
        reminderEvent.askForAmount = false
        reminderEvent.tags = medicine.tags.stream().map { t: Tag? -> t!!.name }.collect((Collectors.toList()))
        reminderEvent.reminderType = Reminder.ReminderType.REFILL

        reminderEvent.amount = "${MedicineHelper.formatAmount(medicine.medicine.amount - medicine.medicine.refillSize, medicine.medicine.unit)} ➡ ${
            MedicineHelper.formatAmount(
                medicine.medicine.amount,
                medicine.medicine.unit
            )
        }"

        return reminderEvent
    }
}