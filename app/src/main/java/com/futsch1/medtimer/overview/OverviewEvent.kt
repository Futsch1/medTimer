package com.futsch1.medtimer.overview

import android.text.Spanned
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.preferences.PreferencesDataSource


abstract class OverviewEvent(private val preferencesDataSource: PreferencesDataSource) {
    abstract val id: Int
    abstract val timestamp: Long
    abstract val text: Spanned
    abstract val icon: Int
    abstract val color: Int?
    abstract val state: OverviewState
    abstract val reminderType: ReminderEntity.ReminderType
    abstract val reminderId: Int
    val updateValue: Long
        get() = if (preferencesDataSource.preferences.value.useRelativeDateTime) System.currentTimeMillis() / 60_000 else 0
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

