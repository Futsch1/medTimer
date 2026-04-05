package com.futsch1.medtimer.overview

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.TextInputDialogBuilder
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.reminders.NotificationProcessor
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class VariableAmountHandler @Inject constructor(
    private val reminderEventRepository: ReminderEventRepository,
    private val notificationProcessor: NotificationProcessor,
    private val reminderNotificationFactory: ReminderNotificationFactory,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun show(activity: AppCompatActivity, intent: Intent) {
        val reminderNotificationData = ReminderNotificationData.fromBundle(intent.extras!!)

        val reminderNotification = withContext(ioDispatcher) {
            reminderNotificationFactory.create(reminderNotificationData)
        } ?: return

        val reminderEvents = mutableListOf<ReminderEvent>()

        for (reminderNotificationPart in reminderNotification.reminderNotificationParts.reversed()) {
            if (!reminderNotificationPart.reminder.variableAmount) {
                reminderEvents.add(reminderNotificationPart.reminderEvent)
                continue
            }

            TextInputDialogBuilder(activity)
                .title(reminderNotificationPart.medicine.name)
                .hint(R.string.dosage)
                .initialText(reminderNotificationPart.reminder.amount)
                .textSink { amountLocal: String? ->
                    amountLocal?.let {
                        activity.lifecycleScope.launch(ioDispatcher) {
                            notificationProcessor.setReminderEventStatus(
                                ReminderEvent.ReminderStatus.TAKEN,
                                listOf(reminderNotificationPart.reminderEvent.copy(amount = it))
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
        reminderEventRepository.update(reminderEvent.copy(processedTimestamp = Instant.now()))
    }
}
