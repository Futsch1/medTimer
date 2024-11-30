package com.futsch1.medtimer.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.futsch1.medtimer.R

class NextRemindersWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val containerViews = RemoteViews(context.packageName, R.layout.next_reminders_widget)

    val views = RemoteViews(context.packageName, R.layout.next_reminders_widget_line)
    views.setTextViewText(R.id.nextReminderWidgetLineText, "Test1")
    containerViews.addView(R.id.nextRemindersWidgetLine1, views)

    val views2 = RemoteViews(context.packageName, R.layout.next_reminders_widget_line)
    views2.setTextViewText(R.id.nextReminderWidgetLineText, "Test2")
    containerViews.addView(R.id.nextRemindersWidgetLine2, views2)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, containerViews)

}