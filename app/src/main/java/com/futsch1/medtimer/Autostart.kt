package com.futsch1.medtimer

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.futsch1.medtimer.LogTags.AUTOSTART
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.ReminderWorkerReceiver
import com.futsch1.medtimer.reminders.ReminderWorkerReceiver.Companion.requestScheduleNextNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import java.util.function.Predicate
import java.util.stream.Collectors

class Autostart : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && (intent.action == "android.intent.action.BOOT_COMPLETED" || intent.action == "android.intent.action.MY_PACKAGE_REPLACED")) {
            Log.i(AUTOSTART, "Requesting reschedule")
            requestScheduleNextNotification(context)
            restoreNotifications(context)
        }
    }

    companion object {
        var hasRestored = false

        fun restoreNotifications(context: Context) {
            if (hasRestored) {
                return
            }
            hasRestored = true

            Log.i(AUTOSTART, "Restore notifications")
            val repo = MedicineRepository(context.applicationContext as Application?)
            val thread = HandlerThread("RestoreNotifications")
            thread.start()
            Handler(thread.getLooper()).post {
                val reminderEventList: List<ReminderEvent> = repo.getLastDaysReminderEvents(1).stream()
                    .filter((Predicate { reminderEvent: ReminderEvent -> reminderEvent.status == ReminderEvent.ReminderStatus.RAISED })).collect(
                        Collectors.toUnmodifiableList()
                    )
                for (reminderEvent in reminderEventList) {
                    val scheduledReminderNotificationData = ReminderNotificationData.fromReminderEvent(reminderEvent)
                    scheduledReminderNotificationData.notificationId = reminderEvent.notificationId
                    Log.i(AUTOSTART, "Restoring reminder event: $scheduledReminderNotificationData")
                    ReminderWorkerReceiver.requestShowReminderNotification(context, scheduledReminderNotificationData)
                }
            }
        }
    }
}
