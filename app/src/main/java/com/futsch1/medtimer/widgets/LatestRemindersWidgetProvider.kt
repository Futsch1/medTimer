package com.futsch1.medtimer.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.futsch1.medtimer.R

class LatestRemindersWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        performWidgetUpdate(
            getLatestReminderWidgetImpl(context), appWidgetIds, appWidgetManager
        )
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}


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