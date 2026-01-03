package com.futsch1.medtimer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.futsch1.medtimer.ActivityCodes.CUSTOM_SNOOZE_ACTIVITY
import com.futsch1.medtimer.ActivityCodes.VARIABLE_AMOUNT_ACTIVITY
import com.futsch1.medtimer.overview.customSnoozeDialog
import com.futsch1.medtimer.overview.variableAmountDialog

fun dispatch(activity: AppCompatActivity, intent: Intent) {
    if (intent.action == VARIABLE_AMOUNT_ACTIVITY) {
        variableAmountDialog(activity, intent)
    }
    if (intent.action == CUSTOM_SNOOZE_ACTIVITY) {
        customSnoozeDialog(activity, intent)
    }
}