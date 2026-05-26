package com.futsch1.medtimer.feature.ui.overview.actions

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.reminders.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.feature.ui.helpers.DeleteHelper
import com.futsch1.medtimer.feature.ui.overview.model.OverviewState
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderEventActions @AssistedInject constructor(
    @Assisted val event: PastReminderEvent,
    @Assisted private val fragmentActivity: FragmentActivity,
    private val reminderEventRepository: ReminderEventRepository,
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val timePickerDialogFactory: TimePickerDialogFactory,
    private val commandBus: ReminderCommandBus,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) : Actions {

    @AssistedFactory
    fun interface Factory {
        fun create(event: PastReminderEvent, fragmentActivity: FragmentActivity): ReminderEventActions
    }

    private val isStockEvent = event.reminderEvent.reminderType in setOf(
        ReminderType.OUT_OF_STOCK, ReminderType.EXPIRATION_DATE, ReminderType.REFILL
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
                Button.ACKNOWLEDGED -> {
                    applicationScope.launch {
                        commandBus.markReminderEvents(listOf(event.reminderEvent.reminderEventId), ReminderEvent.ReminderStatus.ACKNOWLEDGED)
                    }
                }

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
            .create(localDateTime.toLocalTime()) { minutes ->
                val newReminderTime = TimeHelper.changeTimeMinutes(reminderEvent.remindedTimestamp, minutes)
                fragmentActivity.lifecycleScope.launch {
                    val reminderNotificationData = ReminderNotificationData.fromReminderEvent(reminderEvent)
                    reminderNotificationData.remindInstant = newReminderTime
                    reminderNotificationData.notificationId = reminderEvent.notificationId
                    reminderEventRepository.update(reminderEvent.copy(remindedTimestamp = newReminderTime))
                    applicationScope.launch { commandBus.showReminderNotification(reminderNotificationData) }
                }
            }.show(fragmentActivity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
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
        val amount = MedicineHelper.parseAmount(reminderEvent.amount) ?: return
        val reminder = reminderRepository.fetch(reminderEvent.reminderId) ?: return
        medicineRepository.decreaseStock(reminder.medicineRelId, -amount)
    }

    private fun processDeleteReminderEvent(reminderEvent: ReminderEvent) {
        DeleteHelper.deleteItem(fragmentActivity, R.string.are_you_sure_delete_reminder_event, {
            fragmentActivity.lifecycleScope.launch {
                undoStock(reminderEvent)
                reminderEventRepository.update(reminderEvent.copy(status = ReminderEvent.ReminderStatus.DELETED, stockHandled = false))
            }
        }, {})
    }
}
