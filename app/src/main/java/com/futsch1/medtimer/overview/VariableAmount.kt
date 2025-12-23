package com.futsch1.medtimer.overview

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.reminders.NotificationProcessor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

fun variableAmountDialog(
    activity: AppCompatActivity,
    intent: Intent,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val reminderEventId = intent.getIntExtra(ActivityCodes.EXTRA_REMINDER_EVENT_ID, -1)
    val amount = intent.getStringExtra(ActivityCodes.EXTRA_AMOUNT)
    val name = intent.getStringExtra(ActivityCodes.EXTRA_MEDICINE_NAME)!!
    if (reminderEventId != -1) {
        DialogHelper(activity)
            .title(name)
            .hint(R.string.dosage)
            .initialText(amount)
            .textSink { amountLocal: String? ->
                amountLocal?.let {
                    updateReminderEvent(
                        activity,
                        reminderEventId,
                        it,
                        dispatcher
                    )
                }
            }
            .cancelCallback { touchReminderEvent(activity, reminderEventId, dispatcher) }
            .show()
    }
}

private fun touchReminderEvent(
    activity: AppCompatActivity,
    reminderEventId: Int,
    dispatcher: CoroutineDispatcher
) {
    activity.lifecycleScope.launch(dispatcher) {
        val repository = MedicineRepository(activity.application)
        val reminderEvent = repository.getReminderEvent(reminderEventId) ?: return@launch

        reminderEvent.processedTimestamp = Instant.now().epochSecond
        repository.updateReminderEvent(reminderEvent)
    }
}

private fun updateReminderEvent(
    activity: AppCompatActivity,
    reminderEventId: Int,
    amount: String,
    dispatcher: CoroutineDispatcher
) {
    activity.lifecycleScope.launch(dispatcher) {
        Log.d(LogTags.REMINDER, "Update reminder event reID $reminderEventId with variable amount $amount")
        val repository = MedicineRepository(activity.application)
        val reminderEvent = repository.getReminderEvent(reminderEventId) ?: return@launch
        reminderEvent.amount = amount
        NotificationProcessor(activity).setReminderEventStatus(
            ReminderEvent.ReminderStatus.TAKEN,
            listOf(reminderEvent),
        )
    }
}
