package com.futsch1.medtimer.widgets

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WidgetUpdateReceiver : BroadcastReceiver() {
    @Inject
    lateinit var nextRemindersWidgetProvider: NextRemindersWidgetProvider

    @Inject
    lateinit var latestRemindersWidgetProvider: LatestRemindersWidgetProvider


    override fun onReceive(context: Context?, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)

                val appWidgetIdsNextReminders = appWidgetManager.getAppWidgetIds(
                    ComponentName(
                        context!!,
                        NextRemindersWidgetProvider::class.java
                    )
                )
                performWidgetUpdate(
                    nextRemindersWidgetProvider.getWidgetImpl(context), appWidgetIdsNextReminders, appWidgetManager
                )

                val appWidgetIdsLatestReminders = appWidgetManager.getAppWidgetIds(
                    ComponentName(
                        context,
                        LatestRemindersWidgetProvider::class.java
                    )
                )
                performWidgetUpdate(
                    latestRemindersWidgetProvider.getWidgetImpl(context), appWidgetIdsLatestReminders, appWidgetManager
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}