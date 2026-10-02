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
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.resume

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

        for (eventId in reminderNotificationData.reminderEventIds) {
            val event = reminderEventRepository.fetch(eventId) ?: continue
            if (event.status != ReminderEvent.ReminderStatus.RAISED) continue
            val reminder = reminderRepository.fetch(event.reminderId) ?: continue
            if (!reminder.variableAmount) continue
            val medicine = medicineRepository.fetch(reminder.medicineRelId) ?: continue

            suspendCancellableCoroutine { continuation ->
                val title = "${activity.getString(R.string.amount)}: ${medicine.name}"
                TextInputDialogBuilder(activity)
                    .title(title)
                    .hint(R.string.dosage)
                    .initialText(reminder.amount)
                    .textSink { amountLocal: String? ->
                        amountLocal?.let {
                            activity.lifecycleScope.launch(ioDispatcher) {
                                confirmAmount(eventId, it)
                            }
                        }
                        continuation.resume(Unit)
                    }
                    .cancelCallback {
                        activity.lifecycleScope.launch {
                            cancelAmount(eventId)
                        }
                        continuation.resume(Unit)
                    }
                    .show()
            }
        }
    }

    /**
     * Each Taken tap on the notification opens its own dialog, so several can be open for one event.
     * The event is re-read here and only an open dose is taken; writing back the copy read when the
     * dialog opened would reset stockHandled and deduct the stock once per dialog.
     */
    internal suspend fun confirmAmount(eventId: Int, amount: String) {
        val event = openReminderEvent(eventId) ?: return
        reminderEventRepository.update(event.copy(amount = amount))
        commandBus.markReminderEvents(listOf(eventId), ReminderEvent.ReminderStatus.TAKEN)
    }

    /** Leaves an event another dialog already processed untouched instead of reverting it to its earlier state. */
    internal suspend fun cancelAmount(eventId: Int) {
        val event = openReminderEvent(eventId) ?: return
        reminderEventRepository.update(event.copy(processedTimestamp = Instant.now()))
    }

    private suspend fun openReminderEvent(eventId: Int): ReminderEvent? =
        reminderEventRepository.fetch(eventId)?.takeIf { it.status == ReminderEvent.ReminderStatus.RAISED }
}
