package com.futsch1.medtimer

import android.content.Intent
import android.text.InputType
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.api.notificationData.toReminderNotificationData
import com.futsch1.medtimer.feature.ui.helpers.TextInputDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class CustomSnoozeHandler @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val commandBus: ReminderCommandBus,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) {
    suspend fun show(activity: AppCompatActivity, intent: Intent) {
        val reminderNotificationData = intent.extras!!.toReminderNotificationData()
        if (!reminderNotificationData.valid) {
            return
        }
        TextInputDialogBuilder(activity)
            .title(getTitle(activity, reminderNotificationData.reminderIds))
            .hint(com.futsch1.medtimer.core.ui.R.string.minutes_string)
            .initialText("")
            .inputType(InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_NUMBER)
            .textSink { snoozeTime: String? ->
                snoozeTime?.toIntOrNull()?.toDuration(DurationUnit.MINUTES)
                    ?.let { duration ->
                        applicationScope.launch {
                            commandBus.snooze(reminderNotificationData, duration)
                        }
                    }
            }
            .cancelCallback {
                Log.d(LogTags.REMINDER, "Snooze dialog cancelled")
            }
            .show()
    }

    private suspend fun getTitle(activity: AppCompatActivity, reminderIds: List<Int>): String {
        val reminders = reminderIds.mapNotNull { reminderRepository.fetch(it) }
        val medicineNames = reminders.mapNotNull { medicineRepository.fetch(it.medicineRelId) }.toSet().map { it.name }
        return "${activity.getString(com.futsch1.medtimer.core.ui.R.string.snooze_duration)}: ${
            medicineNames.joinToString(
                ", "
            )
        }"
    }
}