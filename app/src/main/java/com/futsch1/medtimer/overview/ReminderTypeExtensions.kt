package com.futsch1.medtimer.overview

import androidx.annotation.DrawableRes
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder

@get:DrawableRes
val Reminder.ReminderType.icon: Int
    get() = when (this) {
        Reminder.ReminderType.TIME_BASED -> R.drawable.calendar_event
        Reminder.ReminderType.CONTINUOUS_INTERVAL -> R.drawable.repeat
        Reminder.ReminderType.LINKED -> R.drawable.link
        Reminder.ReminderType.WINDOWED_INTERVAL -> R.drawable.interval
        Reminder.ReminderType.OUT_OF_STOCK -> R.drawable.box_seam
        Reminder.ReminderType.EXPIRATION_DATE -> R.drawable.ban
        Reminder.ReminderType.REFILL -> R.drawable.cart2
    }
