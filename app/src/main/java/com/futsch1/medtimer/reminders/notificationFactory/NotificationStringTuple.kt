package com.futsch1.medtimer.reminders.notificationFactory

import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.reminders.NotificationTriplet

data class NotificationStringTuple(
    val medicine: FullMedicine,
    val reminder: Reminder
) {

    companion object {
        fun fromNotificationTriplets(triplets: List<NotificationTriplet>): List<NotificationStringTuple> {
            return triplets.map { NotificationStringTuple(it.medicine!!, it.reminder) }
        }
    }
}