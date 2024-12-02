package com.futsch1.medtimer.widgets

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.widget.RemoteViews
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import java.time.LocalDate
import java.time.ZoneId

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

internal fun getNextReminderEvents(context: Context, line: Int): String {
    val medicineRepository = MedicineRepository(context.applicationContext as Application?)
    val medicinesWithReminders = medicineRepository.medicines
    val reminderEvents = medicineRepository.allReminderEventsWithoutDeleted
    val reminderScheduler = ReminderScheduler(object : TimeAccess {
        override fun systemZone(): ZoneId {
            return ZoneId.systemDefault()
        }

        override fun localDate(): LocalDate {
            return LocalDate.now()
        }
    })

    val scheduledReminders = reminderScheduler.schedule(medicinesWithReminders, reminderEvents)

    val scheduledReminder = scheduledReminders.getOrNull(line)

    return if (scheduledReminder != null) scheduledReminderToString(
        context,
        scheduledReminder
    ) else ""
}

private fun scheduledReminderToString(
    context: Context,
    scheduledReminder: ScheduledReminder
): String {
    return TimeHelper.minutesToTimeString(
        context.applicationContext as Application?,
        scheduledReminder.reminder.timeInMinutes.toLong()
    ) +
            ": " + scheduledReminder.reminder.amount + " " + scheduledReminder.medicine.name
}


internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {


    val thread = HandlerThread("UpdateWidget")
    thread.start()
    val handler = Handler(thread.looper)
    handler.post {
        val containerViews = RemoteViews(context.packageName, R.layout.next_reminders_widget)

        createNextReminderWidgetLines(context, containerViews)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, containerViews)
    }
}

fun createNextReminderWidgetLines(context: Context, containerViews: RemoteViews) {
    val viewIds = intArrayOf(
        R.id.nextRemindersWidgetLine1,
        R.id.nextRemindersWidgetLine2,
        R.id.nextRemindersWidgetLine3,
        R.id.nextRemindersWidgetLine4
    )
    for (i in 0..3) {
        val views = RemoteViews(context.packageName, R.layout.next_reminders_widget_line)
        views.setTextViewText(R.id.nextReminderWidgetLineText, getNextReminderEvents(context, i))
        containerViews.addView(viewIds[i], views)
    }
}
