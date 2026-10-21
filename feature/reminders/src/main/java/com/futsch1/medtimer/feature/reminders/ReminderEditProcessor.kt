package com.futsch1.medtimer.feature.reminders

import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.api.scheduling.ReminderEdit
import com.futsch1.medtimer.feature.reminders.api.scheduling.ReminderEditAction
import com.futsch1.medtimer.feature.reminders.api.scheduling.planReminderEdit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries out the actions that [planReminderEdit] decides for the open doses of edited reminders and medicines.
 * It holds no rules of its own. It compares each new medicine list with the previous one; the first list only sets
 * the baseline. Changes of the medicine stock alone are ignored.
 */
@Singleton
class ReminderEditProcessor @Inject constructor(
    private val reminderEventRepository: ReminderEventRepository,
    private val notificationProcessor: NotificationProcessor,
    private val alarmProcessor: AlarmProcessor,
    private val persistentDataDataSource: PersistentDataDataSource,
    private val preferencesDataSource: PreferencesDataSource,
    private val timeAccess: TimeAccess,
    private val commandBus: ReminderCommandBus
) {
    private val mutex = Mutex()
    private var previousMedicines: List<Medicine>? = null

    /** Applies the edits in [medicines] to the open doses, then schedules the next reminder. */
    suspend fun onMedicinesChanged(medicines: List<Medicine>) {
        mutex.withLock {
            val previous = previousMedicines
            previousMedicines = medicines
            if (previous != null) {
                applyEdits(previous, medicines)
            }
        }
        commandBus.scheduleNextNotification()
    }

    private suspend fun applyEdits(previous: List<Medicine>, medicines: List<Medicine>) {
        val edits = findEdits(previous, medicines)
        if (edits.isEmpty()) {
            return
        }

        val startOfToday = timeAccess.localDate().atStartOfDay(timeAccess.systemZone()).toInstant()
        var schedulingEvents: List<ReminderEvent>? = null
        val actions = mutableListOf<ReminderEditAction>()
        for (edit in edits) {
            val openEvents = reminderEventRepository.getOpenFrom(edit.oldReminder.id, startOfToday)
            if (openEvents.isEmpty()) {
                continue
            }
            val events = schedulingEvents ?: reminderEventRepository.getForScheduling(previous + medicines)
                .distinctBy { it.reminderEventId }
                .also { schedulingEvents = it }
            actions += planReminderEdit(edit, openEvents, events, timeAccess, preferencesDataSource)
        }
        execute(actions)
    }

    private fun findEdits(previous: List<Medicine>, medicines: List<Medicine>): List<ReminderEdit> {
        val newMedicines = medicines.associateBy { it.id }
        return previous.flatMap { oldMedicine ->
            val newMedicine = newMedicines[oldMedicine.id]
            val medicineChanged = newMedicine == null || detailsOf(oldMedicine) != detailsOf(newMedicine)
            oldMedicine.reminders.mapNotNull { oldReminder ->
                val newReminder = newMedicine?.reminders?.firstOrNull { it.id == oldReminder.id }
                if (medicineChanged || newReminder != oldReminder) {
                    ReminderEdit(oldMedicine, oldReminder, newMedicine, newReminder)
                } else {
                    null
                }
            }
        }
    }

    /** The medicine without its reminders, stock and sort position, so stock changes and reordering do not count as edits. */
    private fun detailsOf(medicine: Medicine) = medicine.copy(amount = 0.0, sortOrder = 0.0, reminders = emptyList())

    private suspend fun execute(actions: List<ReminderEditAction>) {
        val drops = actions.filterIsInstance<ReminderEditAction.Drop>().map { it.reminderEvent }
        val updates = actions.filter { it is ReminderEditAction.Refresh || it is ReminderEditAction.Retime }.map { it.reminderEvent }

        if (drops.isNotEmpty()) {
            Log.i(LogTags.REMINDER, "Reminder edit drops reIDs ${drops.map { it.reminderEventId }}")
            val dropIds = drops.map { it.reminderEventId }
            dropIds.forEach { alarmProcessor.cancelPendingReminderNotifications(it) }
            persistentDataDataSource.removePendingLocationSnoozesForReminderEventIds(dropIds)
            for ((notificationId, events) in drops.groupBy { it.notificationId }) {
                notificationProcessor.removeRemindersFromNotification(notificationId, events.map { it.reminderEventId }, silent = true)
            }
            drops.forEach { reminderEventRepository.delete(it) }
        }

        if (updates.isNotEmpty()) {
            Log.i(LogTags.REMINDER, "Reminder edit updates reIDs ${updates.map { it.reminderEventId }}")
            updates.forEach { reminderEventRepository.update(it) }
            updates.map { it.notificationId }.distinct().forEach { notificationProcessor.refreshNotification(it) }
        }
    }
}
