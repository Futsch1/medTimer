package com.futsch1.medtimer.overview

import android.text.Spanned
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.helpers.ReminderStringFormatter
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class OverviewScheduledReminderEvent @AssistedInject constructor(
    reminderStringFormatter: ReminderStringFormatter,
    preferencesDataSource: PreferencesDataSource,
    @Assisted val scheduledReminder: ScheduledReminder
) :
    OverviewEvent(preferencesDataSource) {

    @AssistedFactory
    interface Factory {
        fun create(scheduledReminder: ScheduledReminder): OverviewScheduledReminderEvent
    }

    override val text: Spanned = reminderStringFormatter.formatScheduledReminder(scheduledReminder)
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
    override val reminderType: ReminderEntity.ReminderType
        get() = scheduledReminder.reminder.reminderType
    override val reminderId: Int
        get() = scheduledReminder.reminder.reminderId
}