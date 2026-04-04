package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteInputReceiverService @Inject constructor(
    private val notificationProcessor: NotificationProcessor,
    private val reminderEventRepository: ReminderEventRepository,
    private val reminderNotificationFactory: ReminderNotificationFactory,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun handleVariableAmount(
        amountsByReminderEventId: Map<Int, String>,
        reminderNotificationData: ReminderNotificationData
    ) = withContext(ioDispatcher) {
        val reminderNotification = reminderNotificationFactory.create(
            reminderNotificationData
        ) ?: return@withContext

        val reminderEvents = mutableListOf<ReminderEvent>()

        for (reminderNotificationPart in reminderNotification.reminderNotificationParts) {
            if (reminderNotificationPart.reminder.variableAmount) {
                val amount = amountsByReminderEventId[reminderNotificationPart.reminderEvent.reminderEventId] ?: continue
                Log.d(LogTags.REMINDER, "Setting variable amount to $amount of reID ${reminderNotificationPart.reminderEvent.reminderEventId}")

                reminderEvents.add(reminderNotificationPart.reminderEvent)
                reminderEventRepository.update(reminderNotificationPart.reminderEvent.copy(amount = amount))
                continue
            }

            reminderEvents.add(reminderNotificationPart.reminderEvent)
        }

        notificationProcessor.setReminderEventStatus(
            ReminderEvent.ReminderStatus.TAKEN,
            reminderEvents,
        )
    }
}