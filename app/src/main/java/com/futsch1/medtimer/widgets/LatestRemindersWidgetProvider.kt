package com.futsch1.medtimer.widgets

import android.content.Context
import com.futsch1.medtimer.R

class LatestRemindersWidgetProvider : WidgetProvider(::getLatestReminderWidgetImpl)


fun getLatestReminderWidgetImpl(context: Context): WidgetImpl {
    val lineProvider = LatestRemindersLineProvider(context)
    return WidgetImpl(
        context,
        lineProvider,
        WidgetIds(
            R.id.latestReminderWidget,
            R.layout.latest_reminders_widget,
            R.layout.latest_reminders_widget_small
        )
    )
}