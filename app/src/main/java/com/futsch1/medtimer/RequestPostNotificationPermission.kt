package com.futsch1.medtimer

import android.Manifest.permission
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager

class RequestPostNotificationPermission(val activity: AppCompatActivity) {
    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result: Boolean ->
        if (java.lang.Boolean.FALSE == result) {
            PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
                .edit().putBoolean("show_notification", false).apply()
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