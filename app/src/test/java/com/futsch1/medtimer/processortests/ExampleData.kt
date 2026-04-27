package com.futsch1.medtimer.processortests

import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.database.toModel.toEntity
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.schedulertests.TestHelper
import java.time.Instant

fun fillWithTwoReminders(reminderContext: TestReminderContext): ReminderNotificationData {
    reminderContext.repositoryFakes.medicines.add(MedicineEntity("Test").also { it.medicineId = 1 })
    reminderContext.repositoryFakes.reminders.add(TestHelper.buildReminder(1, 1, "1", 600, 1).toEntity())
    reminderContext.repositoryFakes.reminders.add(TestHelper.buildReminder(1, 2, "1", 600, 1).toEntity())
    reminderContext.repositoryFakes.reminderEvents.add(TestHelper.buildReminderEvent(1, 0, 1).toEntity())
    reminderContext.repositoryFakes.reminderEvents.add(TestHelper.buildReminderEvent(2, 0, 2).toEntity())

    return ReminderNotificationData(
        Instant.ofEpochSecond(0),
        listOf(1, 2),
        listOf(1, 2)
    )
}

fun fillWithOneReminder(reminderContext: TestReminderContext): ReminderNotificationData {
    reminderContext.repositoryFakes.medicines.add(MedicineEntity("Test").also { it.medicineId = 1 })
    reminderContext.repositoryFakes.reminders.add(TestHelper.buildReminder(1, 1, "1", 600, 1).toEntity())
    reminderContext.repositoryFakes.reminderEvents.add(TestHelper.buildReminderEvent(1, 0, 1).toEntity())

    return ReminderNotificationData(
        Instant.ofEpochSecond(0),
        listOf(1),
        listOf(1)
    )
}