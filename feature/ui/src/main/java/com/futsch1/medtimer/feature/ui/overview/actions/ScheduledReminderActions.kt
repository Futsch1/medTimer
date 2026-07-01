package com.futsch1.medtimer.feature.ui.overview.actions

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.feature.reminders.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.getVariableAmountActivityIntent
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.ZoneId

class ScheduledReminderActions @AssistedInject constructor(
    @Assisted val scheduledReminder: ScheduledReminder,
    @Assisted private val fragmentActivity: FragmentActivity,
    private val reminderEventCreator: ReminderEventCreator,
    private val timePickerDialogFactory: TimePickerDialogFactory,
    private val commandBus: ReminderCommandBus,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) : Actions {

    @AssistedFactory
    fun interface Factory {
        fun create(event: ScheduledReminder, fragmentActivity: FragmentActivity): ScheduledReminderActions
    }

    private val isStockEvent = scheduledReminder.reminder.isOutOfStockOrExpirationReminder

    override val visibleButtons: MutableList<Button> = mutableListOf()

    init {
        if (isStockEvent) {
            visibleButtons.add(Button.ACKNOWLEDGED)
            visibleButtons.add(Button.RESCHEDULE)
        } else {
            visibleButtons.add(Button.TAKEN)
            visibleButtons.add(Button.SKIPPED)
            visibleButtons.add(Button.RESCHEDULE)
        }
    }

    override suspend fun buttonClicked(button: Button) {
        if (isStockEvent) {
            when (button) {
                Button.ACKNOWLEDGED -> processStockAcknowledged(scheduledReminder)
                Button.RESCHEDULE -> scheduleReminder(scheduledReminder)
                else -> Unit
            }
        } else {
            when (button) {
                Button.TAKEN -> processFutureReminder(scheduledReminder, true)
                Button.SKIPPED -> processFutureReminder(scheduledReminder, false)
                Button.RESCHEDULE -> scheduleReminder(scheduledReminder)
                else -> Unit
            }
        }
    }

    private fun scheduleReminder(scheduledReminder: ScheduledReminder) {
        val initialTime = if (scheduledReminder.reminder.time.isDuration) {
            scheduledReminder.timestamp.atZone(ZoneId.systemDefault()).toLocalTime()
        } else {
            scheduledReminder.reminder.time.getLocalTime()
        }
        timePickerDialogFactory
            .create(initialTime) { minutes ->
                val reminderTimeStamp =
                    TimeHelper.instantFromDateAndMinutes(minutes, scheduledReminder.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()).epochSecond
                fragmentActivity.lifecycleScope.launch {
                    val reminderEvent = reminderEventCreator.getOrCreateReminderEvent(scheduledReminder, reminderTimeStamp)
                    val reminderNotificationData = ReminderNotificationData.fromReminderEvent(reminderEvent)
                    applicationScope.launch { commandBus.showReminderNotification(reminderNotificationData) }
                }
            }.show(fragmentActivity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
    }

    private suspend fun processFutureReminder(scheduledReminder: ScheduledReminder, taken: Boolean) {
        val reminderEvent = reminderEventCreator.getOrCreateReminderEvent(scheduledReminder, scheduledReminder.timestamp.epochSecond)
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
        val reminderEvent = reminderEventCreator.getOrCreateReminderEvent(scheduledReminder, scheduledReminder.timestamp.epochSecond)
        applicationScope.launch { commandBus.markReminderEvents(listOf(reminderEvent.reminderEventId), ReminderEvent.ReminderStatus.ACKNOWLEDGED) }
    }
}
