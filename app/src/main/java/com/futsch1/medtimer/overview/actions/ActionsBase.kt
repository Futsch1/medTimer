package com.futsch1.medtimer.overview.actions

import android.app.Application
import android.view.View
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.ReminderNotificationWorker
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


open class ActionsBase(val view: View, popupWindow: PopupWindow, val ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    val takenButton: ExtendedFloatingActionButton = view.findViewById(R.id.takenButton)
    val skippedButton: ExtendedFloatingActionButton = view.findViewById(R.id.skippedButton)
    val reRaiseOrScheduleButton: ExtendedFloatingActionButton = view.findViewById(R.id.reraiseOrScheduleButton)
    val deleteButton: ExtendedFloatingActionButton = view.findViewById(R.id.deleteButton)
    val anchorTakenButton: View = view.findViewById(R.id.anchorTakenButton)
    val anchorSkippedButton: View = view.findViewById(R.id.anchorSkippedButton)
    val anchorReraiseOrScheduleButton: View = view.findViewById(R.id.anchorReraiseButton)
    val anchorDeleteButton: View = view.findViewById(R.id.anchorDeleteButton)

    init {
        view.setOnClickListener {
            popupWindow.dismiss()
        }
    }

    fun hideDeleteAndReraise() {
        deleteButton.visibility = View.INVISIBLE
        reRaiseOrScheduleButton.visibility = View.INVISIBLE

        setAngle(anchorTakenButton, 70f)
        setAngle(anchorSkippedButton, 110f)
    }

    fun hideDelete() {
        deleteButton.visibility = View.INVISIBLE

        setAngle(anchorTakenButton, 50f)
        setAngle(anchorSkippedButton, 90f)
        setAngle(anchorReraiseOrScheduleButton, 130f)
    }

    protected fun setAngle(view: View, f: Float) {
        val layoutParams = view.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.circleAngle = f
        view.setLayoutParams(layoutParams)
    }

    protected suspend fun createReminderEvent(scheduledReminder: ScheduledReminder, reminderTimeStamp: Long): ReminderEvent {
        return withContext(ioCoroutineDispatcher) {
            val medicineRepository = MedicineRepository(view.context.applicationContext as Application)
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