package com.futsch1.medtimer.overview

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.reminders.NotificationProcessor
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

fun variableAmountDialog(
    activity: AppCompatActivity,
    intent: Intent,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val reminderNotificationData = ReminderNotificationData.fromBundle(intent.extras!!)

    activity.lifecycleScope.launch(dispatcher) {
        val medicineRepository = MedicineRepository(activity.application)
        val reminderNotification = ReminderNotification.fromReminderNotificationData(
            activity,
            medicineRepository,
            reminderNotificationData
        ) ?: return@launch

        val reminderEvents = mutableListOf<ReminderEvent>()

        for (reminderNotificationPart in reminderNotification.reminderNotificationParts) {
            if (!reminderNotificationPart.reminder.variableAmount) {
                reminderEvents.add(reminderNotificationPart.reminderEvent)
            } else {
                DialogHelper(activity)
                    .title(reminderNotificationPart.medicine.medicine.name)
                    .hint(R.string.dosage)
                    .initialText(reminderNotificationPart.reminder.amount)
                    .textSink { amountLocal: String? ->
                        amountLocal?.let {
                            reminderNotificationPart.reminderEvent.amount = it
                            reminderEvents.add(reminderNotificationPart.reminderEvent)
                        }
                    }
                    .cancelCallback { touchReminderEvent(medicineRepository, reminderNotificationPart.reminderEvent) }
                    .show()
            }
        }

        NotificationProcessor(activity).setReminderEventStatus(
            ReminderEvent.ReminderStatus.TAKEN,
            reminderEvents,
        )
    }
}

private fun touchReminderEvent(
    medicineRepository: MedicineRepository,
    reminderEvent: ReminderEvent
) {
    reminderEvent.processedTimestamp = Instant.now().epochSecond
    medicineRepository.updateReminderEvent(reminderEvent)
}
