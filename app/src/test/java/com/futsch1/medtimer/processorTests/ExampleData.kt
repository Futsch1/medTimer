package com.futsch1.medtimer.processorTests

import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.schedulerTests.TestHelper
import java.time.Instant

fun fillWithTwoReminders(reminderContext: TestReminderContext): ReminderNotificationData {
    reminderContext.medicineRepositoryFake.medicines.add(TestHelper.buildFullMedicine(1, "Test").medicine)
    reminderContext.medicineRepositoryFake.reminders.add(TestHelper.buildReminder(1, 1, "1", 600, 1))
    reminderContext.medicineRepositoryFake.reminders.add(TestHelper.buildReminder(1, 2, "1", 600, 1))
    reminderContext.medicineRepositoryFake.reminderEvents.add(TestHelper.buildReminderEvent(1, 0, 1))
    reminderContext.medicineRepositoryFake.reminderEvents.add(TestHelper.buildReminderEvent(2, 0, 2))

    return ReminderNotificationData(
        Instant.ofEpochSecond(0),
        intArrayOf(1, 2),
        intArrayOf(1, 2)
    )
}