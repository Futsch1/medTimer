package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import org.junit.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertTrue

class ReminderSchedulerExpirationTest {
    @Test
    fun scheduleExpirationReminderDisabled() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        medicine.medicine.expirationDate = 0
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1)
        reminder.expirationReminderType = ReminderEntity.ExpirationReminderType.ONCE
        reminder.periodStart = 3

        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicineEntity> = mutableListOf()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())
    }

    @Test
    fun scheduleExpirationReminderOnce() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        medicine.medicine.expirationDate = TestHelper.on(3).toEpochDay()
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1)
        reminder.expirationReminderType = ReminderEntity.ExpirationReminderType.ONCE
        reminder.periodStart = 1

        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicineEntity> = mutableListOf()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())

        reminder.periodStart = 2
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 480),
            medicine.medicine,
            reminder
        )

        reminder.periodStart = 5
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 480),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480)))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())

        scheduledReminders = scheduler.schedule(medicineList, listOf())
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicine.medicine,
            reminder
        )
    }

    @Test
    fun scheduleExpirationReminderDaily() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        medicine.medicine.expirationDate = TestHelper.on(3).toEpochDay()
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1)
        reminder.expirationReminderType = ReminderEntity.ExpirationReminderType.DAILY
        reminder.periodStart = 1

        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicineEntity> = mutableListOf()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicine.medicine,
            reminder
        )

        reminder.periodStart = 2
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 480),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480)))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicine.medicine,
            reminder
        )

        medicine.medicine.expirationDate = TestHelper.on(5).toEpochDay()
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(3, 480),
            medicine.medicine,
            reminder
        )

        reminder.periodStart = 14
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicine.medicine,
            reminder
        )
    }
}