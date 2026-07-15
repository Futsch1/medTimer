package com.futsch1.medtimer.feature.reminders.command

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.feature.reminders.LocationSnoozeProcessor
import com.futsch1.medtimer.feature.reminders.NotificationProcessor
import com.futsch1.medtimer.feature.reminders.RefillProcessor
import com.futsch1.medtimer.feature.reminders.ReminderNotificationProcessor
import com.futsch1.medtimer.feature.reminders.ScheduleNextReminderNotificationProcessor
import com.futsch1.medtimer.feature.reminders.ShowReminderNotificationProcessor
import com.futsch1.medtimer.feature.reminders.SnoozeProcessor
import com.futsch1.medtimer.feature.reminders.StockHandlingProcessor
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationData
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

@Singleton
class ReminderCommandBusImpl @Inject constructor(
    private val notificationProcessor: NotificationProcessor,
    private val snoozeProcessor: SnoozeProcessor,
    private val refillProcessor: RefillProcessor,
    private val reminderNotificationProcessor: ReminderNotificationProcessor,
    private val scheduleNextReminderNotificationProcessor: ScheduleNextReminderNotificationProcessor,
    private val showReminderNotificationProcessor: ShowReminderNotificationProcessor,
    private val stockHandlingProcessor: StockHandlingProcessor,
    private val locationSnoozeProcessor: LocationSnoozeProcessor,
) : ReminderCommandBus {

    override suspend fun scheduleNextNotification() {
        scheduleNextReminderNotificationProcessor.scheduleNextReminder()
    }

    override suspend fun showReminderNotification(data: ReminderNotificationData) {
        showReminderNotificationProcessor.showReminder(data)
    }

    override suspend fun showReminders(data: ReminderNotificationData) {
        if (reminderNotificationProcessor.processReminders(data)) {
            scheduleNextReminderNotificationProcessor.scheduleNextReminder()
        }
    }

    override suspend fun snooze(data: ReminderNotificationData, duration: Duration) {
        snoozeProcessor.processSnooze(data, duration)
    }

    override suspend fun processLocationSnooze(data: ReminderNotificationData) {
        snoozeProcessor.processLocationSnooze(data)
    }

    override suspend fun restoreLocationSnoozes() {
        locationSnoozeProcessor.processLocationSnooze()
    }

    override suspend fun markReminderEvents(reminderEventIds: List<Int>, status: ReminderEvent.ReminderStatus) {
        notificationProcessor.processReminderEventsInNotification(reminderEventIds, status)
        scheduleNextReminderNotificationProcessor.scheduleNextReminder()
    }

    override suspend fun processStockHandling(amount: Double, medicineId: Int, processedEpochSeconds: Long) {
        stockHandlingProcessor.processStock(amount, medicineId, Instant.ofEpochSecond(processedEpochSeconds))
    }

    override suspend fun processRefill(medicineId: Int) {
        refillProcessor.processRefill(medicineId)
    }

    override suspend fun processRefill(reminderEventIds: List<Int>) {
        refillProcessor.processRefill(reminderEventIds)
    }
}
