package com.futsch1.medtimer.feature.ui.overview

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.api.notificationData.toReminderNotificationData
import com.futsch1.medtimer.feature.ui.helpers.TextInputDialogBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

class VariableAmountHandler @Inject constructor(
    private val reminderEventRepository: ReminderEventRepository,
    private val reminderRepository: ReminderRepository,
    private val medicineRepository: MedicineRepository,
    private val commandBus: ReminderCommandBus,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun show(activity: AppCompatActivity, intent: Intent) {
        val reminderNotificationData = intent.extras!!.toReminderNotificationData()
        if (!reminderNotificationData.valid) return

        for (eventId in reminderNotificationData.reminderEventIds.reversed()) {
            val event = reminderEventRepository.fetch(eventId) ?: continue
            val reminder = reminderRepository.fetch(event.reminderId) ?: continue
            if (!reminder.variableAmount) continue
            val medicine = medicineRepository.fetch(reminder.medicineRelId) ?: continue

            TextInputDialogBuilder(activity)
                .title(medicine.name)
                .hint(R.string.dosage)
                .initialText(reminder.amount)
                .textSink { amountLocal: String? ->
                    amountLocal?.let {
                        activity.lifecycleScope.launch(ioDispatcher) {
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
                        touchReminderEvent(event)
                    }
                }
                .show()
        }
    }

    private suspend fun touchReminderEvent(reminderEvent: ReminderEvent) {
        reminderEventRepository.update(reminderEvent.copy(processedTimestamp = Instant.now()))
    }
}
