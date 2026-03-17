package com.futsch1.medtimer

import android.Manifest.permission
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class RequestPostNotificationPermission(val activity: AppCompatActivity, val abortCallback: () -> Unit) {
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