package com.futsch1.medtimer.overview

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.ActivityCodes.EXTRA_AMOUNT
import com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.reminders.NotificationAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

class VariableAmount(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    fun process(activity: AppCompatActivity, intent: Intent) {
        val reminderEventId = intent.getIntExtra(EXTRA_REMINDER_EVENT_ID, -1)
        if (reminderEventId != -1) {
            showDialogToEnterAmount(activity, reminderEventId, intent.getStringExtra(EXTRA_AMOUNT))
        }
    }

    private fun showDialogToEnterAmount(
        activity: AppCompatActivity,
        reminderEventId: Int,
        amount: String?
    ) {
        DialogHelper.showTextInputDialog(
            activity,
            R.string.log_additional_dose,
            R.string.dosage,
            amount,
            { amountLocal: String? ->
                updateReminderEvent(
                    activity,
                    reminderEventId,
                    amountLocal!!
                )
            },
            { touchReminderEvent(activity, reminderEventId) }
        )
    }

    private fun touchReminderEvent(activity: AppCompatActivity, reminderEventId: Int) {
        activity.lifecycleScope.launch(dispatcher) {
            val repository = MedicineRepository(activity.application)
            val reminderEvent = repository.getReminderEvent(reminderEventId)
            reminderEvent?.processedTimestamp = Instant.now().epochSecond
            repository.updateReminderEvent(reminderEvent)
        }
    }

    private fun updateReminderEvent(
        activity: AppCompatActivity,
        reminderEventId: Int,
        amount: String
    ) {
        activity.lifecycleScope.launch(dispatcher) {
            val repository = MedicineRepository(activity.application)
            val reminderEvent = repository.getReminderEvent(reminderEventId)
            reminderEvent?.amount = amount
            NotificationAction.processReminderEvent(
                activity,
                reminderEventId,
                ReminderEvent.ReminderStatus.TAKEN,
                reminderEvent!!,
                repository
            )
        }
    }
}