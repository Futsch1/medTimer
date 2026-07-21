package com.futsch1.medtimer.feature.ui.impl.overview.model

import android.text.Spanned
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.SimulatedReminder
import com.futsch1.medtimer.core.ui.ReminderStringFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class SimulatedReminderEvent @AssistedInject constructor(
    reminderStringFormatter: ReminderStringFormatter,
    preferencesDataSource: PreferencesDataSource,
    @Assisted val simulatedReminder: SimulatedReminder
) :
    OverviewEvent(preferencesDataSource) {

    @AssistedFactory
    fun interface Factory {
        fun create(simulatedReminder: SimulatedReminder): SimulatedReminderEvent
    }

    val scheduledReminder = simulatedReminder.scheduledReminder
    override val text: Spanned = reminderStringFormatter.formatSimulatedReminder(simulatedReminder)
    override val id: Int = java.util.Objects.hash(
        scheduledReminder.reminder.id,
        scheduledReminder.timestamp.epochSecond
    )

    override val timestamp: Long
        get() = scheduledReminder.timestamp.epochSecond
    override val icon: Int
        get() = scheduledReminder.medicine.iconId
    override val color: Int?
        get() = if (scheduledReminder.medicine.useColor) scheduledReminder.medicine.color else null
    override val state: OverviewState
        get() = OverviewState.PENDING
    override val reminderId: Int
        get() = scheduledReminder.reminder.id
}