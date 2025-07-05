package com.futsch1.medtimer.widgets

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import com.futsch1.medtimer.reminders.notificationFactory.ReminderNotificationData
import com.futsch1.medtimer.reminders.notificationFactory.getBigReminderNotificationFactory

class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if ("com.futsch1.medtimer.SHOW_SURFACE".equals(intent?.action)) {
            try {
                val gson: com.google.gson.Gson? = com.google.gson.Gson()
                val reminderData = gson!!.fromJson(intent!!.getStringExtra("com.futsch1.medTimer.REMINDER_ID"),
                    ReminderNotificationData::class.java)
                addPopup(context!!, reminderData)
                return;
            }
            catch (e: Exception){}
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIdsNextReminders = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context!!,
                NextRemindersWidgetProvider::class.java
            )
        )
        performWidgetUpdate(
            getNextReminderWidgetImpl(context), appWidgetIdsNextReminders, appWidgetManager
        )

        val appWidgetIdsLatestReminders = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context,
                LatestRemindersWidgetProvider::class.java
            )
        )
        performWidgetUpdate(
            getLatestReminderWidgetImpl(context), appWidgetIdsLatestReminders, appWidgetManager
        )
    }
    fun addPopup(copyContext: Context, data: ReminderNotificationData) {
        val windowManager = copyContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val bigNotificationFactory = getBigReminderNotificationFactory(copyContext, 1, data)
        bigNotificationFactory.displayBigSurface(copyContext, windowManager) // inflater.inflate(R.layout.notification, dd);
    }

}