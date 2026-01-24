package com.futsch1.medtimer.overview

import android.content.Context
import android.content.SharedPreferences
import android.text.Spanned
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.formatReminderString
import com.futsch1.medtimer.helpers.formatScheduledReminderString
import com.futsch1.medtimer.preferences.PreferencesNames.USE_RELATIVE_DATE_TIME
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder


enum class OverviewState {
    PENDING,
    RAISED,
    TAKEN,
    SKIPPED
}


enum class EventPosition {
    FIRST,

    @Suppress("unused")
    MIDDLE,
    LAST,
    ONLY
}


abstract class OverviewEvent(sharedPreferences: SharedPreferences) {
    val hasRelativeTimes = sharedPreferences.getBoolean(USE_RELATIVE_DATE_TIME, false)

    abstract val id: Int
    abstract val timestamp: Long
    abstract val text: Spanned
    abstract val icon: Int
    abstract val color: Int?
    abstract val state: OverviewState
    val updateValue: Long
        get() = if (hasRelativeTimes) System.currentTimeMillis() / 60_000 else 0
    var eventPosition: EventPosition = EventPosition.MIDDLE


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as OverviewEvent
        return id == other.id && timestamp == other.timestamp && text.toString() == other.text.toString() && icon == other.icon && color == other.color && state == other.state && updateValue == other.updateValue && eventPosition == other.eventPosition
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + icon
        result = 31 * result + (color ?: 0)
        result = 31 * result + state.hashCode()
        return result
    }
}

class OverviewReminderEvent(context: Context, sharedPreferences: SharedPreferences, val reminderEvent: ReminderEvent) : OverviewEvent(sharedPreferences) {
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

class OverviewScheduledReminderEvent(context: Context, sharedPreferences: SharedPreferences, val scheduledReminder: ScheduledReminder) :
    OverviewEvent(sharedPreferences) {
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