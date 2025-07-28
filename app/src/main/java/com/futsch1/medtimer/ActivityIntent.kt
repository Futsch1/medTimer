package com.futsch1.medtimer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.futsch1.medtimer.new_overview.customSnoozeDialog
import com.futsch1.medtimer.new_overview.variableAmountDialog

fun dispatch(activity: AppCompatActivity, intent: Intent) {
    if (intent.action == "VARIABLE_AMOUNT") {
        variableAmountDialog(activity, intent)
    }
    if (intent.action == "CUSTOM_SNOOZE") {
        customSnoozeDialog(activity, intent)
    }
}