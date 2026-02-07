package com.futsch1.medtimer.overview.actions

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.ReminderNotificationWorker
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


abstract class ActionsBase(
    val medicineRepository: MedicineRepository,
    fragmentActivity: FragmentActivity,
    val ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val context: Context = fragmentActivity

    enum class Button {
        TAKEN,
        ACKNOWLEDGED,
        SKIPPED,
        RERAISE,
        RESCHEDULE,
        DELETE
    }

    var visibleButtons: MutableList<Button> = mutableListOf()

    abstract suspend fun buttonClicked(button: Button)

    protected suspend fun createReminderEvent(scheduledReminder: ScheduledReminder, reminderTimeStamp: Long): ReminderEvent {
        return withContext(ioCoroutineDispatcher) {
            var reminderEvent = medicineRepository.getReminderEvent(scheduledReminder.reminder.reminderId, scheduledReminder.timestamp.epochSecond)
            if (reminderEvent == null) {
                reminderEvent = ReminderNotificationWorker.buildReminderEvent(
                    reminderTimeStamp,
                    scheduledReminder.medicine, scheduledReminder.reminder, medicineRepository
                )
            }

            reminderEvent.reminderEventId = medicineRepository.insertReminderEvent(reminderEvent).toInt()

            return@withContext reminderEvent
        }
    }
}