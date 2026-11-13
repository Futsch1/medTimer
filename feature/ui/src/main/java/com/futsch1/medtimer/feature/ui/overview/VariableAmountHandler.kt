package com.futsch1.medtimer.feature.ui.overview

import android.content.Intent
import androidx.fragment.app.FragmentActivity
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
import kotlinx.coroutines.withContext
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
    /**
     * A dose that asks for the dosage. [reminderEventId] returns the id of its event row; for an upcoming dose
     * it creates that row, so it is only called once the amount is confirmed.
     */
    class AmountRequest(
        val medicineName: String,
        val initialAmount: String,
        val reminderEventId: suspend () -> Int
    )

    suspend fun show(activity: FragmentActivity, intent: Intent) {
        val reminderNotificationData = intent.extras!!.toReminderNotificationData()
        if (!reminderNotificationData.valid) return

        val eventsAskingForAmount = withContext(ioDispatcher) { takeDosesWithoutAmount(reminderNotificationData.reminderEventIds) }
        for (event in eventsAskingForAmount) {
            val eventId = event.reminderEventId
            val reminder = reminderRepository.fetch(event.reminderId) ?: continue
            val medicine = medicineRepository.fetch(reminder.medicineRelId) ?: continue

            val amount = askAmount(activity, medicine.name, reminder.amount)
            if (amount != null) {
                activity.lifecycleScope.launch(ioDispatcher) {
                    confirmAmount(eventId, amount)
                }
            } else {
                activity.lifecycleScope.launch {
                    cancelAmount(eventId)
                }
            }
        }
    }

    /**
     * Asks for the amount of each request in turn and takes the dose on OK at [takenTime], or at the current time
     * when it is null. OK takes a Reminded or Skipped dose; a dose that was taken or deleted elsewhere is left alone.
     * A cancelled dialog leaves its dose untouched: an upcoming dose gets no row, and a Reminded or Skipped one keeps
     * its status and processed time, which interval and linked reminders count from.
     */
    suspend fun takeWithAmounts(activity: FragmentActivity, requests: List<AmountRequest>, takenTime: Instant?) {
        for (request in requests) {
            val amount = askAmount(activity, request.medicineName, request.initialAmount) ?: continue
            val eventId = request.reminderEventId()
            withContext(ioDispatcher) {
                takeableReminderEvent(eventId)?.let { take(it, amount, takenTime) }
            }
        }
    }

    /** Shows the amount dialog and returns the entered amount, or null when it is cancelled. */
    internal suspend fun askAmount(activity: FragmentActivity, medicineName: String, initialAmount: String): String? =
        suspendCancellableCoroutine { continuation ->
            TextInputDialogBuilder(activity)
                .title("${activity.getString(R.string.amount)}: $medicineName")
                .hint(R.string.dosage)
                .initialText(initialAmount)
                .textSink { continuation.resume(it) }
                .cancelCallback { continuation.resume(null) }
                .show()
        }

    /**
     * A combined notification opens the amount dialog as soon as one of its doses asks for an amount.
     * Takes the open doses that don't ask right away and returns the open ones that do, so no dose of
     * the notification is left unprocessed.
     */
    internal suspend fun takeDosesWithoutAmount(eventIds: List<Int>): List<ReminderEvent> {
        val openEvents = eventIds.mapNotNull { openReminderEvent(it) }
        val (askingEvents, plainEvents) = openEvents.partition { it.askForAmount }
        if (plainEvents.isNotEmpty()) {
            commandBus.markReminderEvents(plainEvents.map { it.reminderEventId }, ReminderEvent.ReminderStatus.TAKEN)
        }
        return askingEvents
    }

    /**
     * Each Taken tap on the notification opens its own dialog, so several can be open for one event.
     * The event is re-read here and only an open dose is taken; writing back the copy read when the
     * dialog opened would reset stockHandled and deduct the stock once per dialog. The amount is
     * written first, so the stock change deducts it at [takenTime].
     */
    internal suspend fun confirmAmount(eventId: Int, amount: String, takenTime: Instant? = null) {
        val event = openReminderEvent(eventId) ?: return
        take(event, amount, takenTime)
    }

    private suspend fun take(event: ReminderEvent, amount: String, takenTime: Instant?) {
        reminderEventRepository.update(event.copy(amount = amount))
        commandBus.markReminderEvents(listOf(event.reminderEventId), ReminderEvent.ReminderStatus.TAKEN, takenTime)
    }

    /** Leaves an event another dialog already processed untouched instead of reverting it to its earlier state. */
    internal suspend fun cancelAmount(eventId: Int) {
        val event = openReminderEvent(eventId) ?: return
        reminderEventRepository.update(event.copy(processedTimestamp = Instant.now()))
    }

    private suspend fun openReminderEvent(eventId: Int): ReminderEvent? =
        reminderEventRepository.fetch(eventId)?.takeIf { it.status == ReminderEvent.ReminderStatus.RAISED }

    /** The overview can take a Skipped dose too. Statuses are listed explicitly, so a deleted or acknowledged row is never taken. */
    private suspend fun takeableReminderEvent(eventId: Int): ReminderEvent? =
        reminderEventRepository.fetch(eventId)?.takeIf {
            it.status == ReminderEvent.ReminderStatus.RAISED || it.status == ReminderEvent.ReminderStatus.SKIPPED
        }
}
