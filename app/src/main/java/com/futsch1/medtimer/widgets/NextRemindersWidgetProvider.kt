package com.futsch1.medtimer.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.futsch1.medtimer.R

class NextRemindersWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        performWidgetUpdate(
            getNextReminderWidgetImpl(context), appWidgetIds, appWidgetManager
        )
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

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
