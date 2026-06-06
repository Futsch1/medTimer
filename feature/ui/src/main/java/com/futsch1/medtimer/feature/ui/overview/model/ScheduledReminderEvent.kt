package com.futsch1.medtimer.feature.ui.overview.model

import android.text.Spanned
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.ui.ReminderStringFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ScheduledReminderEvent @AssistedInject constructor(
    reminderStringFormatter: ReminderStringFormatter,
    preferencesDataSource: PreferencesDataSource,
    @Assisted val scheduledReminder: ScheduledReminder
) :
    OverviewEvent(preferencesDataSource) {

    @AssistedFactory
    fun interface Factory {
        fun create(scheduledReminder: ScheduledReminder): ScheduledReminderEvent
    }

    override val text: Spanned = reminderStringFormatter.formatScheduledReminder(scheduledReminder)
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