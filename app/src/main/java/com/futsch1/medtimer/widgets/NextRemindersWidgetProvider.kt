package com.futsch1.medtimer.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class NextRemindersWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        performWidgetUpdate(context, appWidgetIds, appWidgetManager)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

fun performWidgetUpdate(
    context: Context,
    appWidgetIds: IntArray,
    appWidgetManager: AppWidgetManager
) {
    val widgetImpl = NextRemindersWidgetImpl(context)
    for (appWidgetId in appWidgetIds) {
        widgetImpl.updateAppWidget(appWidgetManager, appWidgetId)
    }
}

