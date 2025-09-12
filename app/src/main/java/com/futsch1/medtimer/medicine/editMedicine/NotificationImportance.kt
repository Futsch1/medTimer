package com.futsch1.medtimer.medicine.editMedicine

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ReminderNotificationChannelManager
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.helpers.safeStartActivity

fun importanceValueToIndex(medicine: Medicine): Int {
    if (medicine.notificationImportance == ReminderNotificationChannelManager.Importance.DEFAULT.value) {
        return 0
    }
    if (medicine.notificationImportance == ReminderNotificationChannelManager.Importance.HIGH.value) {
        return if (medicine.showNotificationAsAlarm) 2 else 1
    }
    return 0
}

fun importanceIndexToMedicine(index: Int, medicine: Medicine) {
    when (index) {
        0 -> {
            medicine.notificationImportance = ReminderNotificationChannelManager.Importance.DEFAULT.value
            medicine.showNotificationAsAlarm = false
        }

        1 -> {
            medicine.notificationImportance = ReminderNotificationChannelManager.Importance.HIGH.value
            medicine.showNotificationAsAlarm = false
        }

        2 -> {
            medicine.notificationImportance = ReminderNotificationChannelManager.Importance.HIGH.value
            medicine.showNotificationAsAlarm = true
        }
    }
}

fun showEnablePermissionsDialog(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        !(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent()
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.enable_notification_alarm_dialog)
        builder.setPositiveButton(R.string.ok) { _, _ ->
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = "package:${context.packageName}".toUri()
            }
            safeStartActivity(context, intent)
        }

        builder.setNegativeButton(R.string.cancel) { _, _ ->
            // Intentionally empty
        }
        val d = builder.create()
        d.show()
    }
}
