package com.futsch1.medtimer.feature.ui.overview

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationFactory
import com.futsch1.medtimer.feature.reminders.api.notificationData.toReminderNotificationData
import com.futsch1.medtimer.feature.ui.helpers.TextInputDialogBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

class VariableAmountHandler @Inject constructor(
    private val reminderEventRepository: ReminderEventRepository,
    private val commandBus: ReminderCommandBus,
    private val reminderNotificationFactory: ReminderNotificationFactory,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun show(activity: AppCompatActivity, intent: Intent) {
        val reminderNotificationData = intent.extras!!.toReminderNotificationData()

        val reminderNotification = reminderNotificationFactory.create(reminderNotificationData) ?: return

        for (reminderNotificationPart in reminderNotification.reminderNotificationParts.reversed()) {
            if (!reminderNotificationPart.reminder.variableAmount) {
                continue
            }

            TextInputDialogBuilder(activity)
                .title(reminderNotificationPart.medicine.name)
                .hint(R.string.dosage)
                .initialText(reminderNotificationPart.reminder.amount)
                .textSink { amountLocal: String? ->
                    amountLocal?.let {
                        activity.lifecycleScope.launch(ioDispatcher) {
                            val event = reminderNotificationPart.reminderEvent
                            reminderEventRepository.update(event.copy(amount = it))
                            commandBus.markReminderEvents(
                                listOf(event.reminderEventId),
                                ReminderEvent.ReminderStatus.TAKEN
                            )
                        }
                    }
                }
                .cancelCallback {
                    activity.lifecycleScope.launch {
                        touchReminderEvent(reminderNotificationPart.reminderEvent)
                    }
                }
                .show()
        }
    }

    private suspend fun touchReminderEvent(reminderEvent: ReminderEvent) {
        reminderEventRepository.update(reminderEvent.copy(processedTimestamp = Instant.now()))
    }
}
