package com.futsch1.medtimer.overview.actions

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.database.toEntity
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.model.reminderevent.ReminderEventType
import com.futsch1.medtimer.overview.model.OverviewState
import com.futsch1.medtimer.overview.model.PastReminderEvent
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderEventActions @AssistedInject constructor(
    @Assisted val event: PastReminderEvent,
    @Assisted private val fragmentActivity: FragmentActivity,
    private val medicineRepository: MedicineRepository,
    private val timePickerDialogFactory: TimePickerDialogFactory
) : Actions {

    @AssistedFactory
    interface Factory {
        fun create(event: PastReminderEvent, fragmentActivity: FragmentActivity): ReminderEventActions
    }

    private val isStockEvent = event.reminderEvent.reminderType in setOf(
        ReminderEventType.OUT_OF_STOCK, ReminderEventType.EXPIRATION_DATE, ReminderEventType.REFILL
    )

    override val visibleButtons: MutableList<Button> = mutableListOf()

    init {
        if (isStockEvent) {
            if (event.state != OverviewState.RAISED) {
                visibleButtons.add(Button.DELETE)
            } else {
                visibleButtons.add(Button.ACKNOWLEDGED)
            }
        } else {
            if (event.state != OverviewState.TAKEN) {
                visibleButtons.add(Button.TAKEN)
            }
            if (event.state != OverviewState.SKIPPED) {
                visibleButtons.add(Button.SKIPPED)
            }
            if (event.state != OverviewState.RAISED && event.state != OverviewState.PENDING) {
                visibleButtons.add(Button.RERAISE)
                visibleButtons.add(Button.DELETE)
            } else {
                visibleButtons.add(Button.RESCHEDULE)
            }
            if (event.reminderEvent.reminderId == -1) {
                visibleButtons.remove(Button.RERAISE)
            }
        }
    }

    override suspend fun buttonClicked(button: Button) {
        if (isStockEvent) {
            when (button) {
                Button.DELETE -> processDeleteReminderEvent(event.reminderEvent)
                Button.ACKNOWLEDGED -> ReminderProcessorBroadcastReceiver.requestStockReminderAcknowledged(fragmentActivity, event.reminderEvent.toEntity())
                else -> Unit
            }
        } else {
            when (button) {
                Button.TAKEN -> processTakenOrSkipped(event.reminderEvent, true)
                Button.SKIPPED -> processTakenOrSkipped(event.reminderEvent, false)
                Button.RERAISE -> processDeleteReRaiseReminderEvent(event.reminderEvent)
                Button.DELETE -> processDeleteReminderEvent(event.reminderEvent)
                Button.RESCHEDULE -> processPostponeReminder(event.reminderEvent)
                Button.ACKNOWLEDGED -> Unit
            }
        }
    }

    private fun processPostponeReminder(reminderEvent: ReminderEvent) {
        val localDateTime = LocalDateTime.ofInstant(reminderEvent.remindedTimestamp, ZoneId.systemDefault())
        timePickerDialogFactory
            .create(localDateTime.hour, localDateTime.minute) { minutes ->
                val entity = reminderEvent.toEntity()
                val newReminderTime = TimeHelper.changeTimeStampMinutes(entity.remindedTimestamp, minutes)
                fragmentActivity.lifecycleScope.launch {
                    val reminderNotificationData = ReminderNotificationData.fromReminderEvent(entity)
                    reminderNotificationData.remindInstant = Instant.ofEpochSecond(newReminderTime)
                    reminderNotificationData.notificationId = entity.notificationId
                    entity.remindedTimestamp = newReminderTime
                    medicineRepository.updateReminderEvent(entity)
                    ReminderProcessorBroadcastReceiver.requestShowReminderNotification(fragmentActivity, reminderNotificationData)
                }
            }.show(fragmentActivity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
    }

    private fun processTakenOrSkipped(reminderEvent: ReminderEvent, taken: Boolean) {
        ReminderProcessorBroadcastReceiver.requestReminderAction(fragmentActivity, null, reminderEvent.toEntity(), taken)
    }

    private fun processDeleteReRaiseReminderEvent(reminderEvent: ReminderEvent) {
        DeleteHelper.deleteItem(fragmentActivity, R.string.delete_re_raise_event, {
            fragmentActivity.lifecycleScope.launch {
                medicineRepository.deleteReminderEvent(reminderEvent.toEntity())
                ReminderProcessorBroadcastReceiver.requestScheduleNextNotification(fragmentActivity)
            }
        }, {})
    }

    private fun processDeleteReminderEvent(reminderEvent: ReminderEvent) {
        DeleteHelper.deleteItem(fragmentActivity, R.string.are_you_sure_delete_reminder_event, {
            fragmentActivity.lifecycleScope.launch {
                val entity = reminderEvent.toEntity()
                entity.status = ReminderEventEntity.ReminderStatus.DELETED
                medicineRepository.updateReminderEvent(entity)
            }
        }, {})
    }
}
