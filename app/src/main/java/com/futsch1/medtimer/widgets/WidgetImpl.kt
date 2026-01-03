package com.futsch1.medtimer.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        Log.d("WidgetImpl", "Updating widget $appWidgetId: width=$minWidth, height=$minHeight")

        val remoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val viewMapping: Map<SizeF, RemoteViews> = mapOf(
                SizeF(110f, 40f) to createRemoteViews(1, true),
                SizeF(200f, 40f) to createRemoteViews(1, false),
                SizeF(110f, 110f) to createRemoteViews(4, true),
                SizeF(200f, 110f) to createRemoteViews(4, false)
            )
            RemoteViews(viewMapping)
        } else {
            val isSmall = minWidth < 200
            val numLines = if (minHeight < 110) 1 else 4
            createRemoteViews(numLines, isSmall)
        }
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    private fun createRemoteViews(numLines: Int, isShort: Boolean): RemoteViews {
        val layoutId = if (numLines == 1) widgetIds.smallWidgetLayoutId else widgetIds.widgetLayoutId
        val remoteViews = RemoteViews(context.packageName, layoutId)
        createWidgetLines(remoteViews, numLines, isShort)
        remoteViews.setOnClickPendingIntent(
            widgetIds.widgetId,
            getOpenAppPendingIntent()
        )
        return remoteViews
    }

    private fun getOpenAppPendingIntent(): PendingIntent? {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

    }

    private fun createWidgetLines(
        containerViews: RemoteViews,
        numLines: Int,
        isShort: Boolean
    ) {
        val viewIds = intArrayOf(
            R.id.widgetLine1,
            R.id.widgetLine2,
            R.id.widgetLine3,
            R.id.widgetLine4
        )
        for (i in 0..<numLines) {
            val views = RemoteViews(context.packageName, R.layout.widget_line)
            val text = lineProvider.getWidgetLine(i, isShort)
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
