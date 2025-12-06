package com.futsch1.medtimer.reminders

import android.content.Context
import androidx.work.WorkerParameters
import com.futsch1.medtimer.database.ReminderEvent

class SkippedWorker(context: Context, workerParams: WorkerParameters) : ProcessNotificationWorker(context, workerParams, ReminderEvent.ReminderStatus.SKIPPED)