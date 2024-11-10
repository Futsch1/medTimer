package com.futsch1.medtimer

import com.futsch1.medtimer.TestHelper.assertReminded
import com.futsch1.medtimer.TestHelper.assertRemindedAtIndex
import com.futsch1.medtimer.database.MedicineWithReminders
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId

class ReminderSchedulerLinkedUnitTest {
    @Test
    fun test_sourceReminderNotRemindedYet() {
        val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))

        val scheduler = ReminderScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test")
        val reminderSource = TestHelper.buildReminder(1, 1, "1", 480, 1)
        medicineWithReminders.reminders.add(reminderSource)
        val reminderLinked = TestHelper.buildReminder(1, 2, "2", 60, 1)
        reminderLinked.linkedReminderId = 1
        medicineWithReminders.reminders.add(reminderLinked)

        val medicineList: MutableList<MedicineWithReminders> = ArrayList()
        medicineList.add(medicineWithReminders)

        val reminderEventList: List<ReminderEvent> = ArrayList()

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(1, scheduledReminders.size)
        assertEquals(1, scheduledReminders[0].reminder.reminderId)
    }

    @Test
    fun test_scheduleLinkedReminder() {
        val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))

        val scheduler = ReminderScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test")
        val reminderSource = TestHelper.buildReminder(1, 1, "1", 480, 1)
        medicineWithReminders.reminders.add(reminderSource)
        val reminderLinked = TestHelper.buildReminder(1, 2, "2", 60, 1)
        reminderLinked.linkedReminderId = 1
        medicineWithReminders.reminders.add(reminderLinked)

        val medicineList: MutableList<MedicineWithReminders> = ArrayList()
        medicineList.add(medicineWithReminders)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480).epochSecond))
        reminderEventList[0].processedTimestamp = TestHelper.on(1, 481).epochSecond

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertRemindedAtIndex(
            scheduledReminders,
            TestHelper.on(1, 541),
            medicineWithReminders.medicine,
            reminderLinked,
            0
        )
        assertRemindedAtIndex(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicineWithReminders.medicine,
            reminderSource,
            1
        )

        reminderEventList.add(TestHelper.buildReminderEvent(2, TestHelper.on(1, 541).epochSecond))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicineWithReminders.medicine,
            reminderSource
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 480).epochSecond))
        reminderEventList[2].processedTimestamp = TestHelper.on(2, 484).epochSecond
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertRemindedAtIndex(
            scheduledReminders,
            TestHelper.on(2, 544),
            medicineWithReminders.medicine,
            reminderLinked,
            0
        )
    }
}