package com.futsch1.medtimer.reminders.notificationData

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.ReminderNotificationProcessor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ReminderNotificationFactory @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val timeFormatter: TimeFormatter,
    private val preferencesDataSource: PreferencesDataSource
) {
    open suspend fun create(reminderNotificationData: ReminderNotificationData): ReminderNotification? {
        if (!reminderNotificationData.valid) {
            return null
        }

        val result = mutableListOf<ReminderNotificationPart>()

        val numberOfRepeats = preferencesDataSource.preferences.value.numberOfRepetitions
        for (i in reminderNotificationData.reminderIds.indices) {
            val reminder = reminderRepository.get(reminderNotificationData.reminderIds[i])
            if (reminder == null) {
                Log.e(LogTags.REMINDER, String.format("Could not find reminder rID %d in database", reminderNotificationData.reminderIds[i]))
                return null
            }

            val medicine = medicineRepository.getFull(reminder.medicineRelId)
            if (medicine == null) {
                Log.e(LogTags.REMINDER, "Could not find medicine mID ${reminder.medicineRelId} in database")
                return null
            }

            var reminderEvent = if (reminderNotificationData.reminderEventIds[i] != 0) {
                reminderEventRepository.get(reminderNotificationData.reminderEventIds[i])
            } else {
                reminderEventRepository.get(reminder.id, reminderNotificationData.remindInstant.epochSecond)
            }

            if (reminderEvent == null) {
                val newEvent = ReminderNotificationProcessor.buildReminderEvent(
                    reminderNotificationData.remindInstant.epochSecond, medicine, reminder, reminderEventRepository, timeFormatter
                )
                reminderEvent = reminderEventRepository.create(newEvent.copy(remainingRepeats = numberOfRepeats))
            } else {
                reminderNotificationData.notificationId = reminderEvent.notificationId
            }
            reminderNotificationData.reminderEventIds[i] = reminderEvent.reminderEventId
            result.add(ReminderNotificationPart(reminder, reminderEvent, medicine))
        }

        return ReminderNotification(result, reminderNotificationData)
    }
}
