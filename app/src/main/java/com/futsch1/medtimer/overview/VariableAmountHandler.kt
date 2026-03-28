package com.futsch1.medtimer.overview

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.TextInputDialogBuilder
import com.futsch1.medtimer.reminders.NotificationProcessor
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class VariableAmountHandler @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val notificationProcessor: NotificationProcessor,
    private val reminderContext: ReminderContext,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun show(activity: AppCompatActivity, intent: Intent) {
        val reminderNotificationData = ReminderNotificationData.fromBundle(intent.extras!!)

        val reminderNotification = withContext(ioDispatcher) {
            ReminderNotification.fromReminderNotificationData(
                reminderContext,
                reminderNotificationData
            )
        } ?: return

        val reminderEvents = mutableListOf<ReminderEvent>()

        for (reminderNotificationPart in reminderNotification.reminderNotificationParts.reversed()) {
            if (!reminderNotificationPart.reminder.variableAmount) {
                reminderEvents.add(reminderNotificationPart.reminderEvent)
                continue
            }

            TextInputDialogBuilder(activity)
                .title(reminderNotificationPart.medicine.medicine.name)
                .hint(R.string.dosage)
                .initialText(reminderNotificationPart.reminder.amount)
                .textSink { amountLocal: String? ->
                    amountLocal?.let {
                        reminderNotificationPart.reminderEvent.amount = it
                        activity.lifecycleScope.launch(ioDispatcher) {
                            notificationProcessor.setReminderEventStatus(
                                ReminderEvent.ReminderStatus.TAKEN,
                                listOf(reminderNotificationPart.reminderEvent)
                            )
                        }
                    }
                }
                .cancelCallback {
                    activity.lifecycleScope.launch(ioDispatcher) {
                        touchReminderEvent(reminderNotificationPart.reminderEvent)
                    }
                }
                .show()
        }

        withContext(ioDispatcher) {
            notificationProcessor.setReminderEventStatus(
                ReminderEvent.ReminderStatus.TAKEN,
                reminderEvents,
            )
        }
    }

    private suspend fun touchReminderEvent(reminderEvent: ReminderEvent) {
        reminderEvent.processedTimestamp = Instant.now().epochSecond
        medicineRepository.updateReminderEvent(reminderEvent)
    }
}
