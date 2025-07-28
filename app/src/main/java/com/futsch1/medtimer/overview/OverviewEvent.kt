package com.futsch1.medtimer.overview

import android.content.Context
import android.content.SharedPreferences
import android.text.Spanned
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.formatReminderString
import com.futsch1.medtimer.helpers.formatScheduledReminderString


enum class OverviewState {
    PENDING,
    RAISED,
    TAKEN,
    SKIPPED
}

abstract class OverviewEvent() {
    abstract val id: Int
    abstract val timestamp: Long
    abstract val text: Spanned
    abstract val icon: Int
    abstract val color: Int?
    abstract val state: OverviewState
}

class OverviewReminderEvent(context: Context, sharedPreferences: SharedPreferences, val reminderEvent: ReminderEvent) : OverviewEvent() {
    override val text: Spanned = formatReminderString(context, reminderEvent, sharedPreferences)

    override val id: Int
        get() = reminderEvent.reminderEventId
    override val timestamp: Long
        get() = reminderEvent.remindedTimestamp
    override val icon: Int
        get() = reminderEvent.iconId
    override val color: Int?
        get() = if (reminderEvent.useColor) reminderEvent.color else null
    override val state: OverviewState
        get() = mapReminderEventState(reminderEvent.status)

    private fun mapReminderEventState(status: ReminderEvent.ReminderStatus): OverviewState {
        return when (status) {
            ReminderEvent.ReminderStatus.RAISED -> OverviewState.RAISED
            ReminderEvent.ReminderStatus.TAKEN -> OverviewState.TAKEN
            ReminderEvent.ReminderStatus.SKIPPED -> OverviewState.SKIPPED
            else -> OverviewState.PENDING
        }
    }
}

class OverviewScheduledReminderEvent(context: Context, sharedPreferences: SharedPreferences, val scheduledReminder: ScheduledReminder) : OverviewEvent() {
    override val text: Spanned = formatScheduledReminderString(context, scheduledReminder, sharedPreferences)
    override val id: Int
        get() = scheduledReminder.reminder.reminderId + 1_000_000

    override val timestamp: Long
        get() = scheduledReminder.timestamp.epochSecond
    override val icon: Int
        get() = scheduledReminder.medicine.medicine.iconId
    override val color: Int?
        get() = if (scheduledReminder.medicine.medicine.useColor) scheduledReminder.medicine.medicine.color else null
    override val state: OverviewState
        get() = OverviewState.PENDING
}

fun create(context: Context, sharedPreferences: SharedPreferences, reminderEvent: ReminderEvent): OverviewEvent {
    return OverviewReminderEvent(context, sharedPreferences, reminderEvent)
}

fun create(context: Context, sharedPreferences: SharedPreferences, scheduledReminder: ScheduledReminder): OverviewEvent {
    return OverviewScheduledReminderEvent(context, sharedPreferences, scheduledReminder)
}