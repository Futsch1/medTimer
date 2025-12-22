package com.futsch1.medtimer.reminders.notificationData

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.ReminderWorker
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderNotification(val reminderNotificationParts: List<ReminderNotificationPart>, val reminderNotificationData: ReminderNotificationData) {
    fun filterAutomaticallyTaken(): ReminderNotification {
        return filter { !it.reminder.automaticallyTaken }
    }

    fun filterAlreadyProcessed(): ReminderNotification {
        return filter { it.reminderEvent.status == ReminderEvent.ReminderStatus.RAISED }
    }

    private fun filter(predicate: (ReminderNotificationPart) -> Boolean): ReminderNotification {
        val reminderNotificationParts = mutableListOf<ReminderNotificationPart>()
        val removedReminderEventIds = mutableListOf<Int>()
        for (part in this.reminderNotificationParts) {
            if (predicate(part)) {
                reminderNotificationParts.add(part)
            } else {
                removedReminderEventIds.add(part.reminderEvent.reminderEventId)
            }
        }
        return ReminderNotification(
            reminderNotificationParts,
            reminderNotificationData.removeReminderEventIds(removedReminderEventIds)
        )
    }

    fun getLocalDateTime(): LocalDateTime {
        return reminderNotificationData.remindInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    fun getRemindTime(context: Context): String {
        val remindTime = getLocalDateTime()
        return TimeHelper.minutesToTimeString(context, remindTime.hour * 60L + remindTime.minute)
    }

    override fun toString(): String {
        return "ReminderNotification -> $reminderNotificationData"
    }

    companion object {
        fun fromReminderNotificationData(
            context: Context,
            medicineRepository: MedicineRepository,
            reminderNotificationData: ReminderNotificationData
        ): ReminderNotification? {
            if (!reminderNotificationData.valid) {
                return null
            }

            val result = mutableListOf<ReminderNotificationPart>()

            val numberOfRepeats = getNumberOfRepeats(context)
            for (i in reminderNotificationData.reminderIds.indices) {
                val reminder = medicineRepository.getReminder(reminderNotificationData.reminderIds[i])
                if (reminder == null) {
                    Log.e(LogTags.REMINDER, String.format("Could not find reminder rID %d in database", reminderNotificationData.reminderIds[i]))
                    return null
                }

                val medicine = medicineRepository.getMedicine(reminder.medicineRelId)
                if (medicine == null) {
                    Log.e(LogTags.REMINDER, "Could not find medicine mID ${reminder.medicineRelId} in database")
                    return null
                }

                var reminderEvent = getEvent(medicineRepository, reminderNotificationData.reminderEventIds[i], reminder, reminderNotificationData.remindInstant)
                if (reminderEvent == null) {
                    reminderEvent =
                        buildAndInsertReminderEvent(medicineRepository, medicine, reminder, reminderNotificationData.remindInstant, numberOfRepeats)
                    reminderNotificationData.reminderEventIds[i] = reminderEvent.reminderEventId
                } else {
                    Log.d(LogTags.REMINDER, "Reminder event reID ${reminderEvent.reminderEventId} already exists")
                    reminderNotificationData.notificationId = reminderEvent.notificationId
                }
                result.add(ReminderNotificationPart(reminder, reminderEvent, medicine))
            }

            return ReminderNotification(result, reminderNotificationData)
        }

        private fun getEvent(medicineRepository: MedicineRepository, reminderEventId: Int, reminder: Reminder, remindInstant: Instant): ReminderEvent? {
            return if (reminderEventId != 0) {
                medicineRepository.getReminderEvent(reminderEventId)
            } else {
                // We might have created the reminder event already
                medicineRepository.getReminderEvent(reminder.reminderId, remindInstant.epochSecond)
            }
        }

        private fun buildAndInsertReminderEvent(
            medicineRepository: MedicineRepository, medicine: FullMedicine, reminder: Reminder, remindInstant: Instant, numberOfRepeats: Int
        ): ReminderEvent {
            val reminderEvent: ReminderEvent = ReminderWorker.buildReminderEvent(remindInstant.epochSecond, medicine, reminder, medicineRepository)
            reminderEvent.remainingRepeats = numberOfRepeats
            reminderEvent.reminderEventId = medicineRepository.insertReminderEvent(reminderEvent).toInt()
            return reminderEvent
        }

        private fun getNumberOfRepeats(context: Context): Int {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPref.getString(PreferencesNames.NUMBER_OF_REPETITIONS, "3")!!.toInt()
        }

    }
}