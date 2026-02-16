package com.futsch1.medtimer.overview.actions

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.ReminderNotificationProcessor
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


enum class Button(val associatedId: Int) {
    TAKEN(R.id.takenButton),
    ACKNOWLEDGED(R.id.acknowledgedButton),
    SKIPPED(R.id.skippedButton),
    RERAISE(R.id.reraiseButton),
    RESCHEDULE(R.id.rescheduleButton),
    DELETE(R.id.deleteButton);

    companion object {
        fun fromId(id: Int): Button {
            return Button.entries.find { it.associatedId == id } ?: throw IllegalArgumentException("No button with id $id")
        }
    }
}

interface Actions {
    suspend fun buttonClicked(button: Button)

    var visibleButtons: MutableList<Button>

}

abstract class ActionsBase(
    val medicineRepository: MedicineRepository,
    fragmentActivity: FragmentActivity,
    val ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : Actions {
    val context: Context = fragmentActivity

    override var visibleButtons: MutableList<Button> = mutableListOf()

    protected suspend fun createReminderEvent(scheduledReminder: ScheduledReminder, reminderTimeStamp: Long): ReminderEvent {
        return withContext(ioCoroutineDispatcher) {
            var reminderEvent = medicineRepository.getReminderEvent(scheduledReminder.reminder.reminderId, scheduledReminder.timestamp.epochSecond)
            if (reminderEvent == null) {
                reminderEvent = ReminderNotificationProcessor.buildReminderEvent(
                    reminderTimeStamp,
                    scheduledReminder.medicine, scheduledReminder.reminder, medicineRepository
                )

                reminderEvent.reminderEventId = medicineRepository.insertReminderEvent(reminderEvent).toInt()
            }

            return@withContext reminderEvent
        }
    }
}