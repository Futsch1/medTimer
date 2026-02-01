package com.futsch1.medtimer.reminders

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import java.time.Instant
import java.util.stream.Collectors

class RefillWorker(val context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    override fun doWork(): Result {
        val medicineRepository = MedicineRepository(context as Application?)

        val reminderEvent = medicineRepository.getReminderEvent(ProcessedNotificationData.fromData(inputData).reminderEventIds[0])!!
        var medicineId = inputData.getInt(ActivityCodes.EXTRA_MEDICINE_ID, -1)
        if (medicineId == -1) {
            val reminder = medicineRepository.getReminder(reminderEvent.reminderId)
            medicineId = reminder?.medicineRelId ?: -1
        }
        val medicine = medicineRepository.getMedicine(medicineId)
            ?: return Result.failure()

        val refillEvent = processRefill(medicine)
        medicineRepository.updateMedicine(medicine.medicine)
        medicineRepository.insertReminderEvent(refillEvent)
        // Make sure that the database is flushed to avoid races between subsequent stock handling events
        medicineRepository.flushDatabase()

        ReminderWorkerReceiver.requestStockReminderAcknowledged(context, reminderEvent)

        return Result.success()
    }

    fun processRefill(medicine: FullMedicine): ReminderEvent {
        medicine.medicine.amount += medicine.medicine.refillSize
        Log.d(LogTags.REMINDER, "Refill medicine ${medicine.medicine.name} with ${medicine.medicine.refillSize} to amount ${medicine.medicine.amount}")

        // Create refill reminder event
        return buildReminderEvent(medicine)
    }

    fun buildReminderEvent(medicine: FullMedicine): ReminderEvent {
        val reminderEvent = ReminderEvent()
        reminderEvent.reminderId = -1
        reminderEvent.remindedTimestamp = Instant.now().toEpochMilli() / 1000
        reminderEvent.medicineName = medicine.medicine.name
        reminderEvent.color = medicine.medicine.color
        reminderEvent.useColor = medicine.medicine.useColor
        reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN
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