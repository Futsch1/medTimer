package com.futsch1.medtimer.widgets

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId


class NextRemindersWidgetImpl(val context: Context) {
    lateinit var scheduledReminders: List<ScheduledReminder>
    private val job: Job = CoroutineScope(SupervisorJob()).launch {
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

        scheduledReminders = reminderScheduler.schedule(medicinesWithReminders, reminderEvents)
    }

    private fun getNextReminderEvents(
        line: Int
    ): String {
        runBlocking {
            job.join()
        }

        val scheduledReminder = scheduledReminders.getOrNull(line)

        return if (scheduledReminder != null) scheduledReminderToString(
            scheduledReminder
        ) else ""
    }

    private fun scheduledReminderToString(
        scheduledReminder: ScheduledReminder
    ): String {
        val dayString = getDayString(context, scheduledReminder)
        return dayString + TimeHelper.minutesToTimeString(
            context.applicationContext as Application?,
            scheduledReminder.reminder.timeInMinutes.toLong()
        ) +
                ": " + scheduledReminder.reminder.amount + " " + scheduledReminder.medicine.name
    }

    private fun getDayString(context: Context, scheduledReminder: ScheduledReminder): String {
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
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {

        val containerView = RemoteViews(context.packageName, R.layout.next_reminders_widget)
        createNextReminderWidgetLines(containerView, 4)

        val containerViewSmall =
            RemoteViews(context.packageName, R.layout.next_reminders_widget_small)
        createNextReminderWidgetLines(containerViewSmall, 1)

        val viewMapping: Map<SizeF, RemoteViews> = mapOf(
            SizeF(110f, 50f) to containerViewSmall,
            SizeF(110f, 150f) to containerView
        )
        val remoteViews = RemoteViews(viewMapping)
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    private fun createNextReminderWidgetLines(
        containerViews: RemoteViews,
        numLines: Int
    ) {
        val viewIds = intArrayOf(
            R.id.nextRemindersWidgetLine1,
            R.id.nextRemindersWidgetLine2,
            R.id.nextRemindersWidgetLine3,
            R.id.nextRemindersWidgetLine4
        )
        for (i in 0..<numLines) {
            val views = RemoteViews(context.packageName, R.layout.next_reminders_widget_line)
            val text = getNextReminderEvents(i)
            views.setTextViewText(R.id.nextReminderWidgetLineText, text)
            containerViews.addView(viewIds[i], views)
            containerViews.setViewVisibility(
                viewIds[i],
                if (text.isNotEmpty()) View.VISIBLE else View.INVISIBLE
            )
        }
    }
}