package com.futsch1.medtimer.overview

import android.text.Spanned
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.ReminderStringFormatter
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.Instant

class OverviewReminderEvent @AssistedInject constructor(
    reminderStringFormatter: ReminderStringFormatter,
    preferencesDataSource: PreferencesDataSource,
    @Assisted val reminderEvent: ReminderEvent
) :
    OverviewEvent(preferencesDataSource) {

    @AssistedFactory
    interface Factory {
        fun create(reminderEvent: ReminderEvent): OverviewReminderEvent
    }

    override val text: Spanned = reminderStringFormatter.formatReminderEvent(reminderEvent)

    override val id: Int
        get() = reminderEvent.reminderEventId
    override val timestamp: Long
        get() = reminderEvent.remindedTimestamp
    override val icon: Int
        get() = reminderEvent.iconId
    override val color: Int?
        get() = if (reminderEvent.useColor) reminderEvent.color else null
    override val state: OverviewState
        get() = mapReminderEventState(reminderEvent)
    override val reminderType: Reminder.ReminderType
        get() = reminderEvent.reminderType
    override val reminderId: Int
        get() = reminderEvent.reminderId

    private fun mapReminderEventState(reminderEvent: ReminderEvent): OverviewState {
        return when (reminderEvent.status) {
            ReminderEvent.ReminderStatus.RAISED -> {
                if (reminderEvent.remindedTimestamp <= Instant.now().toEpochMilli() / 1000) OverviewState.RAISED else OverviewState.PENDING
            }

            ReminderEvent.ReminderStatus.TAKEN -> OverviewState.TAKEN
            ReminderEvent.ReminderStatus.SKIPPED -> OverviewState.SKIPPED
            ReminderEvent.ReminderStatus.ACKNOWLEDGED -> OverviewState.TAKEN
            ReminderEvent.ReminderStatus.DELETED -> OverviewState.SKIPPED
        }
    }
}