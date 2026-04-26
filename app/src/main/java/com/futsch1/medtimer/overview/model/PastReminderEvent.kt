package com.futsch1.medtimer.overview.model

import android.text.Spanned
import com.futsch1.medtimer.helpers.ReminderStringFormatter
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.Instant

class PastReminderEvent @AssistedInject constructor(
    reminderStringFormatter: ReminderStringFormatter,
    preferencesDataSource: PreferencesDataSource,
    val persistentDataDataSource: PersistentDataDataSource,
    @Assisted val reminderEvent: ReminderEvent
) :
    OverviewEvent(preferencesDataSource) {

    @AssistedFactory
    fun interface Factory {
        fun create(reminderEvent: ReminderEvent): PastReminderEvent
    }

    override val text: Spanned = reminderStringFormatter.formatReminderEvent(reminderEvent)

    override val id: Int
        get() = reminderEvent.reminderEventId
    override val timestamp: Long
        get() = reminderEvent.remindedTimestamp.epochSecond
    override val icon: Int
        get() = reminderEvent.iconId
    override val color: Int?
        get() = if (reminderEvent.useColor) reminderEvent.color else null
    override val state: OverviewState
        get() = mapReminderEventState(reminderEvent)
    override val reminderId: Int
        get() = reminderEvent.reminderId

    private fun mapReminderEventState(reminderEvent: ReminderEvent): OverviewState {
        return when (reminderEvent.status) {
            ReminderEvent.ReminderStatus.RAISED -> {
                if (reminderEvent.remindedTimestamp <= Instant.now()) {
                    if (isLocationSnooze(reminderEvent)) {
                        OverviewState.LOCATION
                    } else {
                        OverviewState.RAISED
                    }
                } else OverviewState.PENDING
            }

            ReminderEvent.ReminderStatus.TAKEN -> OverviewState.TAKEN
            ReminderEvent.ReminderStatus.SKIPPED -> OverviewState.SKIPPED
            ReminderEvent.ReminderStatus.ACKNOWLEDGED -> OverviewState.TAKEN
            ReminderEvent.ReminderStatus.DELETED -> OverviewState.SKIPPED
        }
    }

    private fun isLocationSnooze(reminderEvent: ReminderEvent): Boolean {
        return persistentDataDataSource.getPendingLocationSnoozes().any { it.reminderEventIds.contains(reminderEvent.reminderEventId) }
    }
}
