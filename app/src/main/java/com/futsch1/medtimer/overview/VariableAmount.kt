package com.futsch1.medtimer.overview

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.reminders.NotificationProcessor
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

fun variableAmountDialog(
    activity: AppCompatActivity,
    intent: Intent,
    notificationProcessor: NotificationProcessor,
    reminderContext: ReminderContext,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    val reminderNotificationData = ReminderNotificationData.fromBundle(intent.extras!!)

    activity.lifecycleScope.launch(dispatcher) {
        val medicineRepository = MedicineRepository(activity)
        val reminderNotification = ReminderNotification.fromReminderNotificationData(
            reminderContext,
            reminderNotificationData
        ) ?: return@launch

        val reminderEvents = mutableListOf<ReminderEvent>()

        for (reminderNotificationPart in reminderNotification.reminderNotificationParts.reversed()) {
            if (!reminderNotificationPart.reminder.variableAmount) {
                reminderEvents.add(reminderNotificationPart.reminderEvent)
                continue
            }

            val dialogHelper = DialogHelper(activity)
                .title(reminderNotificationPart.medicine.medicine.name)
                .hint(R.string.dosage)
                .initialText(reminderNotificationPart.reminder.amount)
                .textSink { amountLocal: String? ->
                    amountLocal?.let {
                        reminderNotificationPart.reminderEvent.amount = it
                        activity.lifecycleScope.launch(dispatcher) {
                            notificationProcessor.setReminderEventStatus(
                                ReminderEvent.ReminderStatus.TAKEN,
                                listOf(reminderNotificationPart.reminderEvent)
                            )
                        }
                    }
                }
                .cancelCallback {
                    activity.lifecycleScope.launch {
                        touchReminderEvent(
                            medicineRepository,
                            reminderNotificationPart.reminderEvent
                        )
                    }
                }
            withContext(mainDispatcher) { dialogHelper.show() }
        }

        notificationProcessor.setReminderEventStatus(
            ReminderEvent.ReminderStatus.TAKEN,
            reminderEvents,
        )
    }
}

private suspend fun touchReminderEvent(
    medicineRepository: MedicineRepository,
    reminderEvent: ReminderEvent
) {
    reminderEvent.processedTimestamp = Instant.now().epochSecond
    medicineRepository.updateReminderEvent(reminderEvent)
}
