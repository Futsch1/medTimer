package com.futsch1.medtimer.schedulerTests

import android.content.SharedPreferences
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId

internal class ReminderSchedulerUnitTest {
    @Test
    fun testScheduleEmptyLists() {
        val scheduler: ReminderScheduler = scheduler

        // Two empty lists
        var scheduledReminders: List<ScheduledReminder> = scheduler.schedule(emptyList(), emptyList())
        Assertions.assertTrue(scheduledReminders.isEmpty())

        // One medicine without reminders, no reminder events
        val medicineWithReminders = TestHelper.buildFullMedicine(1, TEST)
        scheduledReminders = scheduler.schedule(listOf(medicineWithReminders), emptyList())
        Assertions.assertTrue(scheduledReminders.isEmpty())

        // No reminder events
        val reminder = TestHelper.buildReminder(1, 1, "1", 12, 1)
        reminder.createdTimestamp = TestHelper.on(1, 13).epochSecond
        medicineWithReminders.reminders.add(reminder)
        scheduledReminders = scheduler.schedule(listOf(medicineWithReminders), emptyList())
        Assertions.assertEquals(1, scheduledReminders.size)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 12), medicineWithReminders.medicine, reminder)
    }

    @Test
    fun testScheduleReminders() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders1 = TestHelper.buildFullMedicine(1, TEST_1)
        val reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1)
        val reminder2 = TestHelper.buildReminder(1, 1, "2", 12, 1)
        medicineWithReminders1.reminders.add(reminder1)
        medicineWithReminders1.reminders.add(reminder2)
        var scheduledReminders: List<ScheduledReminder> = scheduler.schedule(listOf(medicineWithReminders1), emptyList())
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(1, 12), medicineWithReminders1.medicine, reminder2)
        TestHelper.assertRemindedAtIndex(scheduledReminders, TestHelper.on(1, 16), medicineWithReminders1.medicine, reminder1, 1)

        // Now add a second medicine with an earlier reminder
        val medicineWithReminders2 = TestHelper.buildFullMedicine(2, TEST_2)
        val reminder3 = TestHelper.buildReminder(2, 1, "1", 3, 1)
        medicineWithReminders2.reminders.add(reminder3)
        scheduledReminders = scheduler.schedule(listOf(medicineWithReminders1, medicineWithReminders2), emptyList())
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(1, 3), medicineWithReminders2.medicine, reminder3)
    }

    @Test
    fun testScheduleWithEvents() {
        val scheduler: ReminderScheduler = getScheduler(1)

        val medicineWithReminders1 = TestHelper.buildFullMedicine(1, TEST_1)
        val reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1)
        val reminder2 = TestHelper.buildReminder(1, 2, "2", 12, 1)
        medicineWithReminders1.reminders.add(reminder1)
        medicineWithReminders1.reminders.add(reminder2)
        val medicineWithReminders2 = TestHelper.buildFullMedicine(2, TEST_2)
        val reminder3 = TestHelper.buildReminder(2, 3, "1", 3, 1)
        medicineWithReminders2.reminders.add(reminder3)
        val medicineWithReminders = listOf(medicineWithReminders1, medicineWithReminders2)
        // Reminder 3 already invoked
        var scheduledReminders: List<ScheduledReminder> =
            scheduler.schedule(medicineWithReminders, listOf(TestHelper.buildReminderEvent(3, TestHelper.on(2, 3).epochSecond)))
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 12), medicineWithReminders1.medicine, reminder2)
        TestHelper.assertRemindedAtIndex(scheduledReminders, TestHelper.on(2, 16), medicineWithReminders1.medicine, reminder1, 1)
        TestHelper.assertRemindedAtIndex(scheduledReminders, TestHelper.on(3, 3), medicineWithReminders2.medicine, reminder3, 2)

        // Check two reminders at the same time
        val reminder4 = TestHelper.buildReminder(2, 4, "1", 12, 1)
        medicineWithReminders2.reminders.add(reminder4)
        scheduledReminders = scheduler.schedule(
            medicineWithReminders,
            listOf(TestHelper.buildReminderEvent(3, TestHelper.on(2, 3).epochSecond), TestHelper.buildReminderEvent(2, TestHelper.on(2, 12).epochSecond))
        )
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 12), medicineWithReminders2.medicine, reminder4)

        scheduledReminders = scheduler.schedule(
            medicineWithReminders,
            listOf(
                TestHelper.buildReminderEvent(3, TestHelper.on(2, 4).epochSecond),
                TestHelper.buildReminderEvent(2, TestHelper.on(2, 12).epochSecond),
                TestHelper.buildReminderEvent(4, TestHelper.on(2, 12).epochSecond)
            )
        )
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 16), medicineWithReminders1.medicine, reminder1)

        // All reminders already invoked, switch to next day
        scheduledReminders = scheduler.schedule(
            medicineWithReminders, listOf(
                TestHelper.buildReminderEvent(3, TestHelper.on(2, 4).epochSecond + 4 * 60),
                TestHelper.buildReminderEvent(2, TestHelper.on(2, 12).epochSecond),
                TestHelper.buildReminderEvent(1, TestHelper.on(2, 16).epochSecond),
                TestHelper.buildReminderEvent(4, TestHelper.on(2, 16).epochSecond)
            )
        )
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(3, 3), medicineWithReminders2.medicine, reminder3)

        // All reminders already invoked, we are on the next day
        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(2))
        scheduledReminders = scheduler.schedule(
            medicineWithReminders,
            listOf(
                TestHelper.buildReminderEvent(3, TestHelper.on(2, 4).epochSecond),
                TestHelper.buildReminderEvent(2, TestHelper.on(2, 12).epochSecond),
                TestHelper.buildReminderEvent(1, TestHelper.on(2, 16).epochSecond)
            )
        )
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(3, 3), medicineWithReminders2.medicine, reminder3)
    }

    // schedules a reminder for the same day
    @Test
    fun testscheduleSameDayReminder() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders = TestHelper.buildFullMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.createdTimestamp = TestHelper.on(1, 500).epochSecond
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList = emptyList<ReminderEvent>()

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)

        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 480), medicineWithReminders.medicine, reminder)
    }

    // schedules a reminder for a different medicine
    @Test
    fun testScheduleDifferentMedicineReminder() {
        val scheduler: ReminderScheduler = getScheduler(1)

        val medicineWithReminders1 = TestHelper.buildFullMedicine(1, TEST_1)
        val reminder1 = TestHelper.buildReminder(1, 1, "1", 480, 1)
        medicineWithReminders1.reminders.add(reminder1)

        val medicineWithReminders2 = TestHelper.buildFullMedicine(2, TEST_2)
        val reminder2 = TestHelper.buildReminder(2, 2, "2", 480, 1)
        medicineWithReminders2.reminders.add(reminder2)

        val medicineList = listOf(medicineWithReminders1, medicineWithReminders2)

        val reminderEventList = emptyList<ReminderEvent>()

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)

        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 480), medicineWithReminders1.medicine, reminder1)
    }

    // schedules a reminder for every two days
    @Test
    fun testScheduleReminderWithOneDayPause() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders = TestHelper.buildFullMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 2)
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList = mutableListOf<ReminderEvent>()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(1, 480), medicineWithReminders.medicine, reminder)

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480).epochSecond))
        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(3, 480), medicineWithReminders.medicine, reminder)
    }

    // schedules a reminder for every two days
    @Test
    fun testScheduleTwoDayReminderVsOneDay() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders = TestHelper.buildFullMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 2)
        medicineWithReminders.reminders.add(reminder)
        val reminder2 = TestHelper.buildReminder(1, 2, "2", 481, 1)
        medicineWithReminders.reminders.add(reminder2)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList = listOf(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480).epochSecond))

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 481), medicineWithReminders.medicine, reminder2)
    }

    @Test
    fun testScheduleReminderWithOneDayPauseVsThreeDaysPause() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders = TestHelper.buildFullMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 2)
        medicineWithReminders.reminders.add(reminder)
        val reminder2 = TestHelper.buildReminder(1, 2, "2", 481, 4)
        medicineWithReminders.reminders.add(reminder2)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList = mutableListOf(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480).epochSecond))

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(1, 481), medicineWithReminders.medicine, reminder2)

        reminderEventList.add(TestHelper.buildReminderEvent(2, TestHelper.on(1, 481).epochSecond))

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(3, 480), medicineWithReminders.medicine, reminder)

        // On day 3
        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(2))

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(3, 480), medicineWithReminders.medicine, reminder)

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(3, 480).epochSecond))

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(5, 480), medicineWithReminders.medicine, reminder)

        // On day 5
        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(5, 480).epochSecond))

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(5, 481), medicineWithReminders.medicine, reminder2)
    }

    @Test
    fun testScheduleCycleInFuture() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders = TestHelper.buildFullMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 3)
        // Day 4 cycle start day means 5.1.
        reminder.cycleStartDay = 4
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList = ArrayList<ReminderEvent>()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(5, 480), medicineWithReminders.medicine, reminder)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(5, 480), medicineWithReminders.medicine, reminder)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(8, 480), medicineWithReminders.medicine, reminder)

        // Reminder already scheduled for tomorrow
        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(6))
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(8, 480).epochSecond))

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(11, 480), medicineWithReminders.medicine, reminder)
    }

    @Test
    fun testScheduleLongCycleInFuture() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders = TestHelper.buildFullMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 0)
        reminder.consecutiveDays = 90
        reminder.pauseDays = 20

        reminder.cycleStartDay = 4
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList = emptyList<ReminderEvent>()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(5, 480), medicineWithReminders.medicine, reminder)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(5, 480), medicineWithReminders.medicine, reminder)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(6, 480), medicineWithReminders.medicine, reminder)
    }


    @Test
    fun testReminderOverMidnight() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders = TestHelper.buildFullMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 23 * 60 + 45, 1)
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList = ArrayList<ReminderEvent>()
        val reminderEvent = TestHelper.buildReminderEvent(1, TestHelper.on(1, (23 * 60 + 46).toLong()).epochSecond)
        reminderEvent.processedTimestamp = TestHelper.on(2, 1).epochSecond
        reminderEventList.add(reminderEvent)

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, (23 * 60 + 45).toLong()), medicineWithReminders.medicine, reminder)
    }

    @Test
    fun testReminderTomorrow() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders1 = TestHelper.buildFullMedicine(1, TEST_1)
        val reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1)
        medicineWithReminders1.reminders.add(reminder1)

        val medicines = listOf(medicineWithReminders1)

        val reminderEventList =
            listOf(TestHelper.buildReminderEvent(1, TestHelper.on(1, 16).epochSecond), TestHelper.buildReminderEvent(1, TestHelper.on(2, 16).epochSecond))

        val scheduledReminders: List<ScheduledReminder> = scheduler.schedule(medicines, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(3, 16), medicineWithReminders1.medicine, reminder1)
    }

    companion object {
        const val TEST_1: String = "Test1"
        const val TEST: String = "Test"
        const val TEST_2: String = "Test2"

        @JvmStatic
        val scheduler: ReminderScheduler
            get() = getScheduler(0)

        @JvmStatic
        fun getScheduler(plusDays: Int): ReminderScheduler {
            val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
            Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
            Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(plusDays.toLong()))
            val sharedPreferencesMock = Mockito.mock(SharedPreferences::class.java)
            Mockito.`when`(sharedPreferencesMock.getBoolean(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean())).thenReturn(false)

            return ReminderScheduler(mockTimeAccess, sharedPreferencesMock)
        }
    }
}