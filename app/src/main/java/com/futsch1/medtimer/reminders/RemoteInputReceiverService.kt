package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteInputReceiverService @Inject constructor(
    private val notificationProcessor: NotificationProcessor,
    private val medicineRepository: MedicineRepository,
    private val reminderContext: ReminderContext,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun handleVariableAmount(
        amountsByReminderEventId: Map<Int, String>,
        reminderNotificationData: ReminderNotificationData
    ) = withContext(ioDispatcher) {
        val reminderNotification = ReminderNotification.fromReminderNotificationData(
            reminderContext,
            reminderNotificationData
        ) ?: return@withContext

        val reminderEvents = mutableListOf<ReminderEvent>()

        for (reminderNotificationPart in reminderNotification.reminderNotificationParts) {
            if (reminderNotificationPart.reminder.variableAmount) {
                val amount = amountsByReminderEventId[reminderNotificationPart.reminderEvent.reminderEventId] ?: continue
                Log.d(LogTags.REMINDER, "Setting variable amount to $amount of reID ${reminderNotificationPart.reminderEvent.reminderEventId}")

                reminderEvents.add(reminderNotificationPart.reminderEvent)
                reminderNotificationPart.reminderEvent.amount = amount
                medicineRepository.updateReminderEvent(reminderNotificationPart.reminderEvent)
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