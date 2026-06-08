package com.futsch1.medtimer.feature.ui

import android.Manifest.permission
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.futsch1.medtimer.core.common.helpers.safeStartActivity
import com.futsch1.medtimer.core.ui.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun showEnableFullScreenIntentDialog(context: Context, notificationManager: NotificationManager) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || notificationManager.canUseFullScreenIntent()) {
        return
    }
    MaterialAlertDialogBuilder(context)
        .setMessage(R.string.enable_notification_alarm_dialog)
        .setPositiveButton(R.string.ok) { _, _ ->
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = "package:${context.packageName}".toUri()
            }
            safeStartActivity(context, intent)
        }
        .setNegativeButton(R.string.cancel) { _, _ -> }
        .create()
        .show()
}

class RequestPostNotificationPermission(private val activity: AppCompatActivity, private val abortCallback: () -> Unit) {
    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result: Boolean ->
        if (java.lang.Boolean.FALSE == result) {
            abortCallback()
        }
    }

    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                activity, permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(permission.POST_NOTIFICATIONS)
        }
    }
}