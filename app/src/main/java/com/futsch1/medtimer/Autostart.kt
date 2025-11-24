package com.futsch1.medtimer

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.ReminderProcessor.Companion.requestReschedule
import com.futsch1.medtimer.reminders.notifications.Notification
import java.util.function.Predicate
import java.util.stream.Collectors

class Autostart : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && (intent.action == "android.intent.action.BOOT_COMPLETED" || intent.action == "android.intent.action.MY_PACKAGE_REPLACED")) {
            Log.i("Autostart", "Requesting reschedule")
            requestReschedule(context)
            Log.i("Autostart", "Restore reminders")
            restoreReminders(context)
        }
    }

    private fun restoreReminders(context: Context) {
        val repo = MedicineRepository(context.applicationContext as Application?)
        val thread = HandlerThread("RestoreReminders")
        thread.start()
        Handler(thread.getLooper()).post {
            val reminderEventList = repo.getLastDaysReminderEvents(1).stream()
                .filter((Predicate { reminderEvent: ReminderEvent? -> reminderEvent!!.status == ReminderEvent.ReminderStatus.RAISED })).collect(
                    Collectors.toUnmodifiableList()
                )
            for (reminderEvent in reminderEventList) {
                val scheduledNotification = Notification.fromReminderEvent(reminderEvent)
                scheduledNotification.getPendingIntent(context).send()
            }
        }
    }
}
