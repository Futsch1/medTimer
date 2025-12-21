package com.futsch1.medtimer.widgets

import android.content.Context
import com.futsch1.medtimer.R

class NextRemindersWidgetProvider : WidgetProvider(::getNextReminderWidgetImpl)

fun getNextReminderWidgetImpl(context: Context): WidgetImpl {
    val lineProvider = NextRemindersLineProvider(context)
    return WidgetImpl(
        context,
        lineProvider,
        WidgetIds(
            R.id.nextReminderWidget,
            R.layout.next_reminders_widget,
            R.layout.next_reminders_widget_small
        )
    )
}
