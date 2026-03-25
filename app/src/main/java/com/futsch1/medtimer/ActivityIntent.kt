package com.futsch1.medtimer

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.futsch1.medtimer.ActivityCodes.CUSTOM_SNOOZE_ACTIVITY
import com.futsch1.medtimer.ActivityCodes.VARIABLE_AMOUNT_ACTIVITY
import com.futsch1.medtimer.overview.VariableAmountHandler
import com.futsch1.medtimer.overview.customSnoozeDialog

suspend fun dispatch(activity: AppCompatActivity, variableAmountHandler: VariableAmountHandler, intent: Intent) {
    Log.d(LogTags.MAIN, "Dispatch intent: ${intent.action}")
    when (intent.action) {
        VARIABLE_AMOUNT_ACTIVITY -> {
            variableAmountHandler.show(activity, intent)
        }

        CUSTOM_SNOOZE_ACTIVITY -> {
            customSnoozeDialog(activity, intent)
        }
    }
}
