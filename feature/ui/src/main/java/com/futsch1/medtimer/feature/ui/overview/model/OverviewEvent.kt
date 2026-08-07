package com.futsch1.medtimer.feature.ui.overview.model

import com.futsch1.medtimer.core.datastore.PreferencesDataSource

abstract class OverviewEvent(private val preferencesDataSource: PreferencesDataSource) {
    abstract val id: Int
    abstract val timestamp: Long
    abstract val content: OverviewEventContent
    abstract val icon: Int
    abstract val color: Int?
    abstract val state: OverviewState
    abstract val reminderId: Int
    val updateValue: Long
        get() = if (preferencesDataSource.preferences.value.useRelativeDateTime) System.currentTimeMillis() / 60_000 else 0
    abstract val cannotSkipMedicine: Boolean

    val cannotBeSkipped: Boolean
        get() = preferencesDataSource.preferences.value.cannotSkipReminders || cannotSkipMedicine

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as OverviewEvent
        return id == other.id && timestamp == other.timestamp && content == other.content && icon == other.icon && color == other.color && state == other.state && updateValue == other.updateValue
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + icon
        result = 31 * result + (color ?: 0)
        result = 31 * result + state.hashCode()
        return result
    }
}
