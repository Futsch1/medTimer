package com.futsch1.medtimer

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.futsch1.medtimer.ActivityCodes.CUSTOM_SNOOZE_ACTION
import com.futsch1.medtimer.overview.customSnoozeDialog
import com.futsch1.medtimer.overview.variableAmountDialog

fun dispatch(activity: AppCompatActivity, intent: Intent) {
    val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    if (km.isKeyguardLocked) {
        km.requestDismissKeyguard(activity, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                super.onDismissSucceeded()
                dispatchInternal(activity, intent)
            }

            override fun onDismissCancelled() {
                // User cancelled the lock screen, still prepare the activity
                dispatchInternal(activity, intent)
            }
        })
    } else {
        // Device is already unlocked, proceed immediately
        dispatchInternal(activity, intent)
    }
}

private fun dispatchInternal(activity: AppCompatActivity, intent: Intent) {
    if (intent.action == "VARIABLE_AMOUNT") {
        variableAmountDialog(activity, intent)
    }
    if (intent.action == CUSTOM_SNOOZE_ACTION) {
        customSnoozeDialog(activity, intent)
    }
}