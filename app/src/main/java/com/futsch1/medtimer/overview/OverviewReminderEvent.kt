package com.futsch1.medtimer.overview

import android.text.Spanned
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.helpers.ReminderStringFormatter
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.Instant

class OverviewReminderEvent @AssistedInject constructor(
    reminderStringFormatter: ReminderStringFormatter,
    preferencesDataSource: PreferencesDataSource,
    @Assisted val reminderEvent: ReminderEventEntity
) :
    OverviewEvent(preferencesDataSource) {

    @AssistedFactory
    interface Factory {
        fun create(reminderEvent: ReminderEventEntity): OverviewReminderEvent
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
    override val reminderType: ReminderEntity.ReminderType
        get() = reminderEvent.reminderType
    override val reminderId: Int
        get() = reminderEvent.reminderId

    private fun mapReminderEventState(reminderEvent: ReminderEventEntity): OverviewState {
        return when (reminderEvent.status) {
            ReminderEventEntity.ReminderStatus.RAISED -> {
                if (reminderEvent.remindedTimestamp <= Instant.now().toEpochMilli() / 1000) OverviewState.RAISED else OverviewState.PENDING
            }

            ReminderEventEntity.ReminderStatus.TAKEN -> OverviewState.TAKEN
            ReminderEventEntity.ReminderStatus.SKIPPED -> OverviewState.SKIPPED
            ReminderEventEntity.ReminderStatus.ACKNOWLEDGED -> OverviewState.TAKEN
            ReminderEventEntity.ReminderStatus.DELETED -> OverviewState.SKIPPED
        }
    }
}