package com.futsch1.medtimer.feature.ui.overview.actions

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.DoseAmountParser
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.api.getVariableAmountActivityIntent
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData
import com.futsch1.medtimer.feature.ui.helpers.DeleteHelper
import com.futsch1.medtimer.feature.ui.overview.VariableAmountHandler
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class ActionsVisitor @Inject constructor(
    private val reminderEventRepository: ReminderEventRepository,
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val timePickerDialogFactory: TimePickerDialogFactory,
    private val fragmentActivity: FragmentActivity,
    private val reminderEventCreator: ReminderEventCreator,
    private val preferencesDataSource: PreferencesDataSource,
    private val commandBus: ReminderCommandBus,
    private val takenTimePicker: TakenTimePicker,
    private val timeAccess: TimeAccess,
    private val variableAmountHandler: VariableAmountHandler,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) {
    /** One upcoming dose or one dose with an event row, in the order the caller visited them, which is screen order. */
    private sealed interface VisitedEvent {
        data class Scheduled(val scheduledReminder: ScheduledReminder) : VisitedEvent
        data class Past(val pastReminderEvent: PastReminderEvent) : VisitedEvent
    }

    private val visitedEvents: MutableList<VisitedEvent> = mutableListOf()

    fun startVisit(button: Button): AutoCloseable {
        visitedEvents.clear()
        return AutoCloseable {
            fragmentActivity.lifecycleScope.launch {
                complete(button)
            }
        }
    }

    fun visit(scheduledReminder: ScheduledReminder) {
        visitedEvents.add(VisitedEvent.Scheduled(scheduledReminder))
    }

    fun visit(pastReminderEvent: PastReminderEvent) {
        visitedEvents.add(VisitedEvent.Past(pastReminderEvent))
    }

    private suspend fun complete(button: Button) {
        if (button == Button.RESCHEDULE) {
            rescheduleReminders()
        } else if (button == Button.TAKEN_AT) {
            pickTakenTime()
        } else {
            for (visitedEvent in visitedEvents) {
                when (visitedEvent) {
                    is VisitedEvent.Scheduled -> processScheduledReminder(visitedEvent.scheduledReminder, button)
                    is VisitedEvent.Past -> processPastReminderEvent(visitedEvent.pastReminderEvent, button)
                }
            }
        }
    }

    private suspend fun processScheduledReminder(scheduledReminder: ScheduledReminder, button: Button) {
        when (button) {
            Button.ACKNOWLEDGED -> processStockAcknowledged(scheduledReminder)
            Button.TAKEN -> processFutureReminder(scheduledReminder, true)
            Button.SKIPPED -> processFutureReminder(scheduledReminder, false)
            else -> Unit
        }
    }

    private fun processPastReminderEvent(pastReminderEvent: PastReminderEvent, button: Button) {
        when (button) {
            Button.TAKEN -> processTakenOrSkipped(pastReminderEvent.reminderEvent, true)
            Button.SKIPPED -> processTakenOrSkipped(pastReminderEvent.reminderEvent, false)
            Button.RERAISE -> processDeleteReRaiseReminderEvent(pastReminderEvent.reminderEvent)
            Button.DELETE -> processDeleteReminderEvent(pastReminderEvent.reminderEvent)
            Button.ACKNOWLEDGED -> applicationScope.launch {
                commandBus.markReminderEvents(
                    listOf(pastReminderEvent.reminderEvent.reminderEventId),
                    ReminderEvent.ReminderStatus.ACKNOWLEDGED
                )
            }

            // Both open a picker first and are handled in complete.
            Button.RESCHEDULE, Button.TAKEN_AT -> Unit
        }
    }

    private fun rescheduleReminders() {
        if (visitedEvents.isEmpty()) {
            return
        }
        val scheduledReminders = visitedEvents.filterIsInstance<VisitedEvent.Scheduled>().map { it.scheduledReminder }
        val pastReminderEvents = visitedEvents.filterIsInstance<VisitedEvent.Past>().map { it.pastReminderEvent }

        val remindedInstant = Instant.ofEpochSecond(
            pastReminderEvents.firstOrNull()?.timestamp
                ?: scheduledReminders.first().timestamp.epochSecond
        )

        val localDateTime = LocalDateTime.ofInstant(remindedInstant, ZoneId.systemDefault())
        timePickerDialogFactory
            .create(localDateTime.toLocalTime()) { minutes ->
                val newReminderTime = TimeHelper.changeTimeMinutes(remindedInstant, minutes)

                fragmentActivity.lifecycleScope.launch {
                    val pendingReminderEvents = scheduledReminders.map { scheduledReminder ->
                        reminderEventCreator.resolvePending(scheduledReminder, newReminderTime.epochSecond)
                    } + pastReminderEvents.map { pastReminderEvent ->
                        pastReminderEvent.reminderEvent.copy(remindedTimestamp = newReminderTime)
                    }
                    val newReminderEvents = reminderEventRepository.upsertMany(pendingReminderEvents)

                    applicationScope.launch { commandBus.scheduleNextNotification() }

                    if (preferencesDataSource.preferences.value.combineNotifications) {
                        requestShowReminders(newReminderEvents, newReminderTime)
                    } else {
                        newReminderEvents.forEach {
                            requestShowReminders(listOf(it), newReminderTime)
                        }
                    }
                }
            }.show(fragmentActivity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
    }

    /**
     * One picker serves every visited event, so a whole group or a whole selection records the
     * same taken time. The visited events are copied first: a later visit clears the list while
     * the picker is still open.
     */
    private fun pickTakenTime() {
        if (visitedEvents.isEmpty()) {
            return
        }
        val events = visitedEvents.toList()

        takenTimePicker.pick(timeAccess.now()) { takenTime ->
            fragmentActivity.lifecycleScope.launch {
                take(events, takenTime)
            }
        }
    }

    /**
     * Marks the doses that don't ask for the dosage at once, so a cancelled amount dialog does not lose [takenTime]
     * for them. Then each dose that asks gets its amount dialog in screen order. An upcoming one gets its row only
     * on OK: a left-over RAISED row would keep the scheduler from ever notifying it.
     */
    private suspend fun take(events: List<VisitedEvent>, takenTime: Instant) {
        val (askingEvents, plainEvents) = events.partition { it.asksForAmount() }
        val reminderEventIds = plainEvents.filterIsInstance<VisitedEvent.Scheduled>().map {
            createReminderEvent(it.scheduledReminder).reminderEventId
        } + plainEvents.filterIsInstance<VisitedEvent.Past>().map { it.pastReminderEvent.reminderEvent.reminderEventId }
        if (reminderEventIds.isNotEmpty()) {
            applicationScope.launch {
                commandBus.markReminderEvents(reminderEventIds, ReminderEvent.ReminderStatus.TAKEN, takenTime)
            }
        }
        if (askingEvents.isNotEmpty()) {
            variableAmountHandler.takeWithAmounts(fragmentActivity, askingEvents.map { it.toAmountRequest() }, takenTime)
        }
    }

    /**
     * A row asks whenever its reminder asks for the dosage: the overview amount dialog takes Reminded and Skipped
     * rows, and Taken rows never offer Taken.
     */
    private fun VisitedEvent.asksForAmount(): Boolean = when (this) {
        is VisitedEvent.Scheduled -> scheduledReminder.reminder.variableAmount
        is VisitedEvent.Past -> pastReminderEvent.reminderEvent.askForAmount
    }

    private fun VisitedEvent.toAmountRequest(): VariableAmountHandler.AmountRequest = when (this) {
        is VisitedEvent.Scheduled -> VariableAmountHandler.AmountRequest(scheduledReminder.medicine.name, scheduledReminder.reminder.amount) {
            createReminderEvent(scheduledReminder).reminderEventId
        }

        is VisitedEvent.Past -> pastReminderEvent.reminderEvent.let { reminderEvent ->
            VariableAmountHandler.AmountRequest(reminderEvent.medicineName, reminderEvent.amount) { reminderEvent.reminderEventId }
        }
    }

    private suspend fun createReminderEvent(scheduledReminder: ScheduledReminder): ReminderEvent =
        reminderEventCreator.getOrCreateReminderEvent(scheduledReminder, scheduledReminder.timestamp.epochSecond)

    private fun requestShowReminders(reminderEvents: List<ReminderEvent>, remindInstant: Instant) {
        val reminderIds = reminderEvents.map { it.reminderId }
        val reminderEventIds = reminderEvents.map { it.reminderEventId }
        val reminderNotificationData = ReminderNotificationData.fromArrays(
            reminderIds,
            reminderEventIds,
            remindInstant
        )
        applicationScope.launch { commandBus.showReminderNotification(reminderNotificationData) }
    }

    private fun processTakenOrSkipped(reminderEvent: ReminderEvent, taken: Boolean) {
        val status = if (taken) ReminderEvent.ReminderStatus.TAKEN else ReminderEvent.ReminderStatus.SKIPPED
        applicationScope.launch { commandBus.markReminderEvents(listOf(reminderEvent.reminderEventId), status) }
    }

    private fun processDeleteReRaiseReminderEvent(reminderEvent: ReminderEvent) {
        DeleteHelper.deleteItem(fragmentActivity, R.string.delete_re_raise_event, {
            fragmentActivity.lifecycleScope.launch {
                undoStock(reminderEvent)
                reminderEventRepository.delete(reminderEvent)
                applicationScope.launch { commandBus.scheduleNextNotification() }
            }
        }, {})
    }

    private suspend fun undoStock(reminderEvent: ReminderEvent) {
        if (reminderEvent.stockHandled) {
            val amount = DoseAmountParser.parseAmount(reminderEvent.amount) ?: return
            val reminder = reminderRepository.fetch(reminderEvent.reminderId) ?: return
            medicineRepository.decreaseStock(reminder.medicineRelId, -amount)
        }
    }

    private fun processDeleteReminderEvent(reminderEvent: ReminderEvent) {
        DeleteHelper.deleteItem(fragmentActivity, R.string.are_you_sure_delete_reminder_event, {
            fragmentActivity.lifecycleScope.launch {
                undoStock(reminderEvent)
                reminderEventRepository.update(
                    reminderEvent.copy(
                        status = ReminderEvent.ReminderStatus.DELETED,
                        stockHandled = false
                    )
                )
            }
        }, {})
    }

    private suspend fun processFutureReminder(
        scheduledReminder: ScheduledReminder,
        taken: Boolean
    ) {
        val reminderEvent = reminderEventCreator.getOrCreateReminderEvent(
            scheduledReminder,
            scheduledReminder.timestamp.epochSecond
        )
        if (taken && scheduledReminder.reminder.variableAmount) {
            fragmentActivity.startActivity(
                getVariableAmountActivityIntent(
                    fragmentActivity,
                    ReminderNotificationData.fromReminderEvent(reminderEvent)
                )
            )
        } else {
            val status = if (taken) ReminderEvent.ReminderStatus.TAKEN else ReminderEvent.ReminderStatus.SKIPPED
            applicationScope.launch { commandBus.markReminderEvents(listOf(reminderEvent.reminderEventId), status) }
        }
    }

    private suspend fun processStockAcknowledged(scheduledReminder: ScheduledReminder) {
        val reminderEvent = reminderEventCreator.getOrCreateReminderEvent(
            scheduledReminder,
            scheduledReminder.timestamp.epochSecond
        )
        applicationScope.launch {
            commandBus.markReminderEvents(
                listOf(reminderEvent.reminderEventId),
                ReminderEvent.ReminderStatus.ACKNOWLEDGED
            )
        }
    }
}
