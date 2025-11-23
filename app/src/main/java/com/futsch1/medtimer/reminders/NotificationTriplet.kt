package com.futsch1.medtimer.reminders

import android.os.Bundle
import androidx.work.Data
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent

class NotificationTriplet(val reminder: Reminder, var reminderEvent: ReminderEvent, val medicine: FullMedicine?) {
    companion object {

        fun getReminderIds(inputData: Data): IntArray {
            return inputData.getIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST)!!
        }

        fun getReminderIds(bundle: Bundle): IntArray {
            return bundle.getIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST)!!
        }

        fun getReminderEventIds(inputData: Data): IntArray {
            return inputData.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)!!
        }

        fun getReminderEventIds(bundle: Bundle): IntArray {
            return bundle.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)!!
        }

        fun fromBundle(bundle: Bundle, medicineRepository: MedicineRepository): List<NotificationTriplet> {
            val reminderIds = getReminderIds(bundle)
            val reminderEventIds = getReminderEventIds(bundle)

            return fromArrays(reminderIds, reminderEventIds, medicineRepository)
        }

        fun getReminderIds(triplets: List<NotificationTriplet>): IntArray {
            return triplets.map { it.reminder.reminderId }.toIntArray()
        }

        fun getReminderEventIds(triplets: List<NotificationTriplet>): IntArray {
            return triplets.map { it.reminderEvent.reminderEventId }.toIntArray()
        }

        fun fromArrays(reminderIds: IntArray, reminderEventIds: IntArray, medicineRepository: MedicineRepository): List<NotificationTriplet> {
            val result = mutableListOf<NotificationTriplet>()

            for (i in reminderIds.indices) {
                val reminder = medicineRepository.getReminder(reminderIds[i])
                val reminderEvent = medicineRepository.getReminderEvent(reminderEventIds[i])!!
                val medicine = medicineRepository.getMedicine(reminder.medicineRelId)

                result.add(NotificationTriplet(reminder, reminderEvent, medicine))
            }

            return result
        }

        fun toBundle(bundle: Bundle, notificationTriplets: List<NotificationTriplet>) {
            val reminderIds = notificationTriplets.map { it.reminder.reminderId }.toIntArray()
            val reminderEventIds = notificationTriplets.map { it.reminderEvent.reminderEventId }.toIntArray()
            bundle.putIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST, reminderIds)
            bundle.putIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, reminderEventIds)
        }
    }

    override fun toString(): String {
        return "NotificationTriplet(reminder=$reminder, reminderEvent=$reminderEvent, medicine=$medicine)"
    }
}