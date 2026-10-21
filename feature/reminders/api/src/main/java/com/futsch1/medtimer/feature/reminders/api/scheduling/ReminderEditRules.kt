package com.futsch1.medtimer.feature.reminders.api.scheduling

import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderType
import java.time.Instant

/*
 * All rules for how an edit of a reminder or its medicine changes the doses that already exist.
 * Keep every such rule in this file; the executor (ReminderEditProcessor) only carries out the actions.
 *
 * User-facing rule (docs/help/scheduling-reminders.md):
 * When you change a reminder or its medicine, MedTimer updates every dose of that reminder that is still open and
 * due today or later. Doses that you already marked as taken or skipped do not change. Missed doses from earlier
 * days do not change either, so your history stays as it was. A dose that you moved with Reschedule keeps the time
 * you picked, but it shows the new amount and name.
 *
 * Rule table, per open (RAISED) dose due from the start of today:
 * - Reminder inactive, reminder deleted or medicine deleted: Drop.
 * - Time set by you (Reschedule, or any dose on a later day): keep the time, Refresh if the details changed.
 * - Time set by the schedule (equal to the time the old settings give with this dose left out):
 *   - new time null or not today: Drop
 *   - new time equal: Keep, or Refresh if the details changed
 *   - new time not after now: Retime (silent)
 *   - new time after now: Drop (the scheduler raises it at the new time, combined with same-time reminders)
 * Interval and linked reminders use the same scheduler call, so the new time is the last resolved dose plus the
 * new interval, or the source processed time plus the new delay.
 */

sealed interface ReminderEditAction {
    val reminderEvent: ReminderEvent

    /** Nothing to do for [reminderEvent]. */
    data class Keep(override val reminderEvent: ReminderEvent) : ReminderEditAction

    /** Store [reminderEvent] with refreshed details and rebuild its posted notification silently. */
    data class Refresh(override val reminderEvent: ReminderEvent) : ReminderEditAction

    /** Store [reminderEvent] with its new time and details and rebuild its posted notification silently. */
    data class Retime(override val reminderEvent: ReminderEvent) : ReminderEditAction

    /** Remove [reminderEvent] from its notification, cancel its alarms and delete it. */
    data class Drop(override val reminderEvent: ReminderEvent) : ReminderEditAction
}

/**
 * Copies the medicine and reminder details that a dose shows. New doses ([com.futsch1.medtimer.feature.reminders.api.buildReminderEvent])
 * and refreshed doses use this one list. Stock values are never copied. The amount of out-of-stock and expiration
 * doses is derived from the medicine stock or date and is left as it is.
 */
fun ReminderEvent.withSnapshotFields(medicine: Medicine, reminder: Reminder): ReminderEvent {
    val keepsAmount = reminder.reminderType == ReminderType.OUT_OF_STOCK || reminder.reminderType == ReminderType.EXPIRATION_DATE
    return copy(
        medicineName = medicine.name + CyclesHelper.getCycleCountString(reminder),
        amount = if (keepsAmount) amount else reminder.amount,
        askForAmount = reminder.variableAmount,
        color = medicine.color,
        useColor = medicine.useColor,
        iconId = medicine.iconId,
        tags = medicine.tags.map { it.name },
        cannotBeSkipped = medicine.cannotBeSkipped
    )
}

/**
 * One reminder before and after an edit. [newMedicine] or [newReminder] is null if it was deleted.
 */
data class ReminderEdit(
    val oldMedicine: Medicine,
    val oldReminder: Reminder,
    val newMedicine: Medicine?,
    val newReminder: Reminder?
)

/**
 * Plans what happens to the open doses of the edited reminder.
 *
 * [openEvents] are the stored doses of the reminder; doses that are not open or are due before today are ignored
 * and get no action. [schedulingEvents] are the events the scheduler sees.
 */
fun planReminderEdit(
    edit: ReminderEdit,
    openEvents: List<ReminderEvent>,
    schedulingEvents: List<ReminderEvent>,
    timeAccess: TimeAccess,
    dataSource: PreferencesDataSource
): List<ReminderEditAction> {
    val (oldMedicine, oldReminder, newMedicine, newReminder) = edit
    val today = timeAccess.localDate().toEpochDay()
    val zone = timeAccess.systemZone()
    val inScope = openEvents.filter {
        it.reminderId == oldReminder.id &&
                it.status == ReminderEvent.ReminderStatus.RAISED &&
                it.remindedTimestamp.atZone(zone).toLocalDate().toEpochDay() >= today
    }

    return inScope.map { event ->
        if (newMedicine == null || newReminder == null || !newReminder.active) {
            return@map ReminderEditAction.Drop(event)
        }
        val refreshed = event.withSnapshotFields(newMedicine, newReminder)
        val otherEvents = schedulingEvents.filter { it.reminderEventId != event.reminderEventId }
        val isToday = TimeHelper.isOnDay(event.remindedTimestamp.epochSecond, today, zone)
        val setBySchedule = isToday &&
                nextTime(oldReminder, oldMedicine, otherEvents, timeAccess, dataSource) == event.remindedTimestamp
        if (!setBySchedule) {
            return@map refreshOrKeep(event, refreshed)
        }

        val newTime = nextTime(newReminder, newMedicine, otherEvents, timeAccess, dataSource)
        when {
            newTime == null || !TimeHelper.isOnDay(newTime.epochSecond, today, zone) -> ReminderEditAction.Drop(event)
            newTime == event.remindedTimestamp -> refreshOrKeep(event, refreshed)
            !newTime.isAfter(timeAccess.now()) -> ReminderEditAction.Retime(refreshed.copy(remindedTimestamp = newTime))
            else -> ReminderEditAction.Drop(event)
        }
    }
}

private fun refreshOrKeep(event: ReminderEvent, refreshed: ReminderEvent): ReminderEditAction =
    if (refreshed != event) ReminderEditAction.Refresh(refreshed) else ReminderEditAction.Keep(event)

private fun nextTime(
    reminder: Reminder,
    medicine: Medicine,
    events: List<ReminderEvent>,
    timeAccess: TimeAccess,
    dataSource: PreferencesDataSource
): Instant? = SchedulingFactory().create(reminder, medicine, events, timeAccess, dataSource).getNextScheduledTime()
