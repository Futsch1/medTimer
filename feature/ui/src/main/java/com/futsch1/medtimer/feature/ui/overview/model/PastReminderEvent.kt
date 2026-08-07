package com.futsch1.medtimer.feature.ui.overview.model

import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.Duration

class PastReminderEvent @AssistedInject constructor(
    preferencesDataSource: PreferencesDataSource,
    val persistentDataDataSource: PersistentDataDataSource,
    private val timeAccess: TimeAccess,
    @Assisted val reminderEvent: ReminderEvent
) :
    OverviewEvent(preferencesDataSource) {

    @AssistedFactory
    fun interface Factory {
        fun create(reminderEvent: ReminderEvent): PastReminderEvent
    }

    private val preferences = preferencesDataSource.preferences.value

    override val content: OverviewEventContent = OverviewEventContent(
        reminderType = reminderEvent.reminderType,
        time = reminderEvent.remindedTimestamp,
        medicineName = reminderEvent.medicineName,
        dose = reminderEvent.amount,
        takenTime = takenTime(reminderEvent),
        interval = interval(reminderEvent),
        stock = stockChange(reminderEvent),
        useRelativeTime = preferences.useRelativeDateTime,
    )

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
    override val cannotSkipMedicine: Boolean
        get() = reminderEvent.cannotBeSkipped

    private fun takenTime(reminderEvent: ReminderEvent) =
        reminderEvent.processedTimestamp.takeIf {
            it.epochSecond != 0L && reminderEvent.isTaken && preferences.showTakenTimeInOverview
        }

    private fun interval(reminderEvent: ReminderEvent): Duration? {
        val lastIntervalMinutes = reminderEvent.lastIntervalReminderTimeInMinutes
        if (lastIntervalMinutes <= 0 || reminderEvent.status != ReminderEvent.ReminderStatus.TAKEN) return null
        val elapsed = Duration.ofSeconds(reminderEvent.processedTimestamp.epochSecond - lastIntervalMinutes * 60L)
        return elapsed.takeIf { !it.isNegative }
    }

    private fun stockChange(reminderEvent: ReminderEvent) =
        if (reminderEvent.stockBefore == reminderEvent.stockAfter) {
            null
        } else {
            StockChange(reminderEvent.stockBefore, reminderEvent.stockAfter, reminderEvent.stockUnit)
        }

    private fun mapReminderEventState(reminderEvent: ReminderEvent): OverviewState {
        return when (reminderEvent.status) {
            ReminderEvent.ReminderStatus.RAISED -> {
                if (reminderEvent.remindedTimestamp <= timeAccess.now()) {
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
        return persistentDataDataSource.getPendingLocationSnoozes()
            .any { it.reminderEventIds.contains(reminderEvent.reminderEventId) }
    }
}

private val ReminderEvent.isTaken: Boolean
    get() = status == ReminderEvent.ReminderStatus.TAKEN || status == ReminderEvent.ReminderStatus.ACKNOWLEDGED
