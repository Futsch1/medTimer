package com.futsch1.medtimer.feature.ui.overview.actions

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
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
    @param:ApplicationScope private val applicationScope: CoroutineScope
) {
    private val visitScheduledReminder: MutableList<ScheduledReminder> = mutableListOf()
    private val visitedPastReminderEvents: MutableList<PastReminderEvent> = mutableListOf()

    fun startVisit(button: Button): AutoCloseable {
        visitScheduledReminder.clear()
        visitedPastReminderEvents.clear()
        return AutoCloseable {
            fragmentActivity.lifecycleScope.launch {
                complete(button)
            }
        }
    }

    fun visit(scheduledReminder: ScheduledReminder) {
        visitScheduledReminder.add(scheduledReminder)
    }

    fun visit(pastReminderEvent: PastReminderEvent) {
        visitedPastReminderEvents.add(pastReminderEvent)
    }

    private suspend fun complete(button: Button) {
        if (button == Button.RESCHEDULE) {
            rescheduleReminders()
        } else {
            for (scheduledReminder in visitScheduledReminder) {
                when (button) {
                    Button.ACKNOWLEDGED -> processStockAcknowledged(scheduledReminder)
                    Button.TAKEN -> processFutureReminder(scheduledReminder, true)
                    Button.SKIPPED -> processFutureReminder(scheduledReminder, false)
                    else -> Unit
                }
            }
            for (pastReminderEvent in visitedPastReminderEvents) {
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

                    else -> Unit
                }
            }
        }
    }

    private suspend fun createReminderEvents(
        scheduledReminders: List<ScheduledReminder>,
        remindedInstant: Instant
    ): List<ReminderEvent> {
        return scheduledReminders.map { scheduledReminder ->
            reminderEventCreator.getOrCreateReminderEvent(
                scheduledReminder,
                remindedInstant.epochSecond
            )
        }
    }


    private fun rescheduleReminders() {
        if (visitedPastReminderEvents.isEmpty() && visitScheduledReminder.isEmpty()) {
            return
        }

        val remindedInstant = Instant.ofEpochSecond(
            visitedPastReminderEvents.firstOrNull()?.timestamp
                ?: visitScheduledReminder.first().timestamp.epochSecond
        )

        val localDateTime = LocalDateTime.ofInstant(remindedInstant, ZoneId.systemDefault())
        timePickerDialogFactory
            .create(localDateTime.toLocalTime()) { minutes ->
                val newReminderTime = TimeHelper.changeTimeMinutes(remindedInstant, minutes)

                fragmentActivity.lifecycleScope.launch {
                    val newReminderEvents =
                        createReminderEvents(
                            visitScheduledReminder,
                            newReminderTime
                        ).toMutableList()
                    for (pastReminderEvent in visitedPastReminderEvents) {
                        val newReminderEvent =
                            pastReminderEvent.reminderEvent.copy(remindedTimestamp = newReminderTime)
                        reminderEventRepository.update(newReminderEvent)
                        newReminderEvents.add(newReminderEvent)
                    }

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
            val amount = MedicineHelper.parseAmount(reminderEvent.amount) ?: return
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
                getVariableAmountActivityIntent(fragmentActivity, ReminderNotificationData.fromReminderEvent(reminderEvent))
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
            commandBus.markReminderEvents(listOf(reminderEvent.reminderEventId), ReminderEvent.ReminderStatus.ACKNOWLEDGED)
        }
    }
}
