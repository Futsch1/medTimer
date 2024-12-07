package com.futsch1.medtimer.widgets

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.SizeF
import android.widget.RemoteViews
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import java.time.LocalDate
import java.time.ZoneId

internal fun getNextReminderEvents(context: Context, line: Int): String {
    val medicineRepository = MedicineRepository(context.applicationContext as Application?)
    val medicinesWithReminders = medicineRepository.medicines
    val reminderEvents = medicineRepository.allReminderEventsWithoutDeleted
    val reminderScheduler = ReminderScheduler(object : ReminderScheduler.TimeAccess {
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
    val dayString = getDayString(context, scheduledReminder)
    return dayString + TimeHelper.minutesToTimeString(
        context.applicationContext as Application?,
        scheduledReminder.reminder.timeInMinutes.toLong()
    ) +
            ": " + scheduledReminder.reminder.amount + " " + scheduledReminder.medicine.name
}

fun getDayString(context: Context, scheduledReminder: ScheduledReminder): String {
    val reminderDate = scheduledReminder.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
    return if (reminderDate == LocalDate.now()) {
        ""
    } else {
        TimeHelper.toLocalizedDateString(
            context,
            scheduledReminder.timestamp.toEpochMilli() / 1000
        ) + " "
    }
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

        val containerView = RemoteViews(context.packageName, R.layout.next_reminders_widget)
        createNextReminderWidgetLines(context, containerView, 4)

        val containerViewSmall =
            RemoteViews(context.packageName, R.layout.next_reminders_widget_small)
        createNextReminderWidgetLines(context, containerViewSmall, 1)

        val viewMapping: Map<SizeF, RemoteViews> = mapOf(
            SizeF(110f, 50f) to containerViewSmall,
            SizeF(110f, 150f) to containerView
        )
        val remoteViews = RemoteViews(viewMapping)
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }
}

fun createNextReminderWidgetLines(context: Context, containerViews: RemoteViews, numLines: Int) {
    val viewIds = intArrayOf(
        R.id.nextRemindersWidgetLine1,
        R.id.nextRemindersWidgetLine2,
        R.id.nextRemindersWidgetLine3,
        R.id.nextRemindersWidgetLine4
    )
    for (i in 0..<numLines) {
        val views = RemoteViews(context.packageName, R.layout.next_reminders_widget_line)
        views.setTextViewText(R.id.nextReminderWidgetLineText, getNextReminderEvents(context, i))
        containerViews.addView(viewIds[i], views)
    }
}