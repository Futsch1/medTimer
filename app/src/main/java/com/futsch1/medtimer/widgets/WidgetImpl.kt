package com.futsch1.medtimer.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.futsch1.medtimer.MainActivity
import com.futsch1.medtimer.R


data class WidgetIds(
    val widgetId: Int,
    val widgetLayoutId: Int,
    val smallWidgetLayoutId: Int
)

class WidgetImpl(
    val context: Context,
    private val lineProvider: WidgetLineProvider,
    private val widgetIds: WidgetIds
) {
    internal fun updateAppWidget(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val containerView = RemoteViews(context.packageName, widgetIds.widgetLayoutId)
        createNextReminderWidgetLines(containerView, 4)
        containerView.setOnClickPendingIntent(
            widgetIds.widgetId,
            getOpenAppPendingIntent()
        )

        val containerViewSmall =
            RemoteViews(context.packageName, widgetIds.smallWidgetLayoutId)
        createNextReminderWidgetLines(containerViewSmall, 1)
        containerViewSmall.setOnClickPendingIntent(
            widgetIds.widgetId,
            getOpenAppPendingIntent()
        )

        val viewMapping: Map<SizeF, RemoteViews> = mapOf(
            SizeF(110f, 50f) to containerViewSmall,
            SizeF(110f, 150f) to containerView
        )
        val remoteViews = RemoteViews(viewMapping)
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    private fun getOpenAppPendingIntent(): PendingIntent? {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

    }

    private fun createNextReminderWidgetLines(
        containerViews: RemoteViews,
        numLines: Int
    ) {
        val viewIds = intArrayOf(
            R.id.widgetLine1,
            R.id.widgetLine2,
            R.id.widgetLine3,
            R.id.widgetLine4
        )
        for (i in 0..<numLines) {
            val views = RemoteViews(context.packageName, R.layout.widget_line)
            val text = lineProvider.getWidgetLine(i)
            views.setTextViewText(R.id.widgetLineText, text)
            containerViews.addView(viewIds[i], views)
            containerViews.setViewVisibility(
                viewIds[i],
                if (text.isNotEmpty()) View.VISIBLE else View.INVISIBLE
            )
        }
    }
}

fun performWidgetUpdate(
    widgetImpl: WidgetImpl,
    appWidgetIds: IntArray,
    appWidgetManager: AppWidgetManager
) {
    for (appWidgetId in appWidgetIds) {
        widgetImpl.updateAppWidget(appWidgetManager, appWidgetId)
    }
}
