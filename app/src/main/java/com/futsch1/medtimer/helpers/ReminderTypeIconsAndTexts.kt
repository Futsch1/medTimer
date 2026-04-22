package com.futsch1.medtimer.helpers

import com.futsch1.medtimer.R
import com.futsch1.medtimer.model.ReminderType

fun ReminderType.getIcon(): Int {
    return when (this) {
        ReminderType.TIME_BASED -> R.drawable.calendar_event
        ReminderType.LINKED -> R.drawable.link
        ReminderType.CONTINUOUS_INTERVAL -> R.drawable.repeat
        ReminderType.WINDOWED_INTERVAL -> R.drawable.interval
        ReminderType.OUT_OF_STOCK -> R.drawable.box_seam
        ReminderType.EXPIRATION_DATE -> R.drawable.ban
        ReminderType.REFILL -> R.drawable.cart2
    }
}

fun ReminderType.getTitle(): Int {
    return when (this) {
        ReminderType.TIME_BASED -> R.string.time_based_reminder
        ReminderType.LINKED -> R.string.linked_reminder
        ReminderType.CONTINUOUS_INTERVAL -> R.string.continuous_interval_reminder
        ReminderType.WINDOWED_INTERVAL -> R.string.windowed_interval_reminder
        ReminderType.OUT_OF_STOCK -> R.string.out_of_stock_reminder
        ReminderType.EXPIRATION_DATE -> R.string.expiration_date
        ReminderType.REFILL -> R.string.refill
    }
}

fun ReminderType.getHelp(): Int {
    return when (this) {
        ReminderType.TIME_BASED -> R.string.time_based_reminder_help
        ReminderType.LINKED -> R.string.linked_reminder_help
        ReminderType.CONTINUOUS_INTERVAL -> R.string.continuous_interval_reminder_help
        ReminderType.WINDOWED_INTERVAL -> R.string.windowed_interval_reminder_help
        ReminderType.OUT_OF_STOCK -> R.string.out_of_stock_reminder_help
        ReminderType.EXPIRATION_DATE -> R.string.expiration_date_reminder_help
        ReminderType.REFILL -> R.string.refill
    }
}