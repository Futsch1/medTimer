package com.futsch1.medtimer.reminders

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import java.time.Instant
import java.time.ZoneId

class AllTakenWork(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val reminderEventId = inputData.getInt(ActivityCodes.EXTRA_REMINDER_EVENT_ID, 0)
        val medicineRepository = MedicineRepository(applicationContext as Application)
        val reminderEvent = medicineRepository.getReminderEvent(reminderEventId)
        if (reminderEvent != null) {
            val allReminders = medicineRepository.getSameTimeReminders(reminderEvent.reminderId)
            val dayOfYear = Instant.ofEpochSecond(reminderEvent.remindedTimestamp).atZone(ZoneId.systemDefault()).dayOfYear
            val allReminderEventIds: List<ReminderEvent?> = allReminders.map { medicineRepository.getLastReminderEvent(it.reminderId) } + reminderEvent
            allReminderEventIds.forEach {
                it?.let { it1 ->
                    if (Instant.ofEpochSecond(it1.remindedTimestamp).atZone(ZoneId.systemDefault()).dayOfYear == dayOfYear) {
                        NotificationAction.processNotification(applicationContext, it1.reminderEventId, ReminderEvent.ReminderStatus.TAKEN)
                    }
                }
            }
            Log.d(LogTags.REMINDER, "All taken: {$allReminderEventIds}")
            medicineRepository.flushDatabase()
        }

        return Result.success()
    }
}