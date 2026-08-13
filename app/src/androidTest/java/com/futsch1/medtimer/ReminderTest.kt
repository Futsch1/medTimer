package com.futsch1.medtimer

import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.R
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import java.text.DateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


@HiltAndroidTest
class ReminderTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun activeReminderTest() {
        val futureTime = Calendar.getInstance()
        val year = futureTime.get(Calendar.YEAR)
        futureTime.set(year + 1, 1, 1)
        val pastTime = Calendar.getInstance()
        pastTime.set(year - 1, 1, 1)

        seed.medicine("Test") { reminder("1", laterToday()) }

        medicines.clickItem(0)
        reminders.inSettingsOf(0) { toggleEnabled() }

        navigation.toOverview()
        overview.assertEventCount(0)

        navigation.toMedicines()
        medicines.clickItem(0)

        reminders.inSettingsOf(0) { toggleEnabled() }

        navigation.toOverview()
        overview.assertEventCount(1)

        navigation.toMedicines()
        medicines.clickItem(0)

        reminders.inSettingsOf(0) {
            inPeriod {
                enablePeriodStart()
                setPeriodStartDate(futureTime.time)
            }
        }

        navigation.toOverview()
        overview.assertEventCount(0)

        navigation.toMedicines()
        medicines.clickItem(0)
        reminders.inSettingsOf(0) {
            inPeriod {
                enablePeriodEnd()
                setPeriodEndDate(pastTime.time)
            }
        }

        navigation.toOverview()
        overview.assertEventCount(0)

        navigation.toMedicines()
        medicines.clickItem(0)
        reminders.inSettingsOf(0) {
            inPeriod {
                setPeriodStartDate(pastTime.time)
                setPeriodEndDate(futureTime.time)
            }
        }

        navigation.toOverview()
        overview.assertEventCount(1)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun activeIntervalReminderTest() {
        val futureTime = Calendar.getInstance()
        val year = futureTime.get(Calendar.YEAR)
        futureTime.set(year + 1, 1, 1)
        val nowTime = Calendar.getInstance()

        seed.medicine("Test") { intervalReminder("1", 180.minutes) }

        medicines.clickItem(0)
        reminders.inSettingsOf(0) {
            setIntervalStart(
                futureTime.time,
                LocalTime.of(futureTime.get(Calendar.HOUR_OF_DAY), futureTime.get(Calendar.MINUTE))
            )
            toggleEnabled()
        }

        medicines.clickItem(0)
        menus.clickEditMedicineOption(R.string.activate_all)

        reminders.inSettingsOf(0) {
            assertIntervalStart(DateFormat.getDateInstance(DateFormat.SHORT).format(nowTime.getTime()))
        }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun deleteLinkedReminderTest() {
        medicines.create("Test med")
        medicineEditor.addReminder("1", earlierToday())

        reminders.addLinkedReminder(0, minutes = 1)
        reminders.addLinkedReminder(1, minutes = 2)

        reminders.delete(0)

        // Check that the reminder list is empty
        reminders.assertCount(0)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun reminderTypeTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        medicines.create("Test")

        // Standard time based reminder (amount 1); leaves room for the linked reminder 30 minutes later
        val reminder1Time = laterToday(40.minutes, headroom = 30.minutes)
        medicineEditor.addReminder("1", reminder1Time)

        // Linked reminder (amount 2) 30 minutes later
        reminders.addLinkedReminder(0, amount = "2", minutes = 30)

        // Interval reminder (amount 3) 2 hours from now
        medicineEditor.addHourlyIntervalReminder("3", interval = 2.hours)

        // Windowed interval reminder (amount 4)
        medicineEditor.addWindowedIntervalReminder(
            "4",
            windowStart = laterToday(5.minutes),
            windowEnd = LocalTime.of(23, 59),
            interval = 3.hours
        )

        // Check calendar view not crashing
        calendar.assertOpens()

        reminders.assertCount(4)
        reminders.assertContains(
            context.getString(R.string.every_interval, "2 " + context.resources.getQuantityString(R.plurals.hours, 2))
        )
        reminders.assertContainsTime(timeFormatter().minutesToTimeString(reminder1Time.toSecondOfDay() / 60))
        reminders.assertContains(
            context.getString(R.string.linked_reminder_summary, timeFormatter().toTimeString(reminder1Time))
        )
        reminders.assertContains(
            context.getString(R.string.every_interval, "3 " + context.resources.getQuantityString(R.plurals.hours, 3))
        )

        // Each reminder's settings must open without crashing
        repeat(4) { reminders.reopenSettings(it) }

        // Check overview and next reminders
        navigation.toOverview()

        overview.assertEventContains("Test (1)")
        overview.assertEventContains(timeFormatter().minutesToTimeString(reminder1Time.toSecondOfDay() / 60))

        overview.assertEventContains("Test (3)")

        overview.assertEventContains("Test (4)")

        // If possible, take reminder 1 now and see if reminder 2 appears
        overview.clickEventState(1)
        overview.clickAction(R.string.taken)

        overview.assertEventContains("Test (2)")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun editReminderTest() {
        seed.medicine("Test")

        navigation.toOverview()

        manualDose.log("Test", amount = "12")
        val now = Instant.now()

        eventEditor.forEvent(0) {
            assertName("Test")
            assertAmount("12")
            assertRemindedAt(timeFormatter().toTimeString(now), timeFormatter().toDateString(now))
            assertTakenAt(timeFormatter().toTimeString(now), timeFormatter().toDateString(now))
            assertNotes("")

            markSkipped()
            setNotes("Test notes")
        }

        overview.assertEventState(0, R.string.skipped)

        val newReminded = now.plusSeconds(60 * 60 * 24 + 120)
        val newTaken = now.plusSeconds(60 * 60 * 48 + 180)

        eventEditor.forEvent(0) {
            markTaken()
            assertNotes("Test notes")

            setRemindedAt(timeFormatter().toTimeString(newReminded), timeFormatter().toDateString(newReminded))
            setTakenAt(timeFormatter().toTimeString(newTaken), timeFormatter().toDateString(newTaken))
        }

        navigation.toAnalysis()

        statistics.selectView(StatisticFragment.TABLE)
        statistics.assertTableContains(timeFormatter().toDateTimeString(newReminded))
        statistics.assertTableContains(timeFormatter().toDateTimeString(newTaken))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun deleteReminderTest() {
        seed.medicine("Test") { reminder("1", laterToday()) }

        navigation.toOverview()

        overview.clickEventState(0)
        overview.clickAction(R.string.taken)

        overview.clickEventState(0)
        overview.clickAction(R.string.delete)
        dialogs.confirm()

        overview.assertEventCount(0)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun intervalReminderTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        seed.medicine("Test") { intervalReminder("1", intervalWithinToday(10.minutes)) }

        navigation.toOverview()

        overview.clickEventState(0)
        overview.clickAction(R.string.taken)

        overview.assertNoEventContains(context.getString(R.string.interval_time, "0 min"))

        overview.clickEventState(1)
        overview.clickAction(R.string.taken)

        overview.assertEventContains(context.getString(R.string.interval_time, "0 min"))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun cyclicReminderTest() {
        val reminderCycles: Array<CyclicReminderInfo> = arrayOf(
            CyclicReminderInfo(1, 0, false),
            CyclicReminderInfo(1, 1, false),
            CyclicReminderInfo(1, 2, false),
            CyclicReminderInfo(2, 0, false),
            CyclicReminderInfo(2, 1, true),
        )

        // Create medicine
        val medicineId = seed.medicine("Test")

        for (cycle in reminderCycles) {
            // Create reminder
            seed.remindersOf(medicineId) { reminder("1", laterToday()) }

            val cycleStart = Calendar.getInstance()

            medicines.clickItem(0)
            reminders.inSettingsOf(0) {
                inCycle {
                    setConsecutiveDays(cycle.consecutiveDays)
                    setPauseDays(cycle.pauseDays)
                    setCycleStartDate(cycleStart.time)
                }
            }

            // Mark event as taken
            navigation.toOverview()
            overview.clickEventState(0)
            overview.clickAction(R.string.taken)

            // Check if cyclic information is present
            eventEditor.forEvent(0) {
                if (cycle.shouldHaveInfo) {
                    assertName(String.format("Test (1/%d)", cycle.consecutiveDays))
                } else {
                    assertNameDoesNotContain("Test (")
                    assertName("Test")
                }
            }

            // Remove event
            overview.clickEventState(0)
            overview.clickAction(R.string.delete)
            dialogs.confirm()

            // Remove reminder
            navigation.toMedicines()
            medicines.clickItem(0)
            reminders.delete(0)
        }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun weekendMode() {
        val windowStart = frozenNowCapped(headroom = 30.minutes)
        val reminderTime = windowStart.plusMinutes(15)
        val windowEnd = windowStart.plusMinutes(30)

        settings.inSection(R.string.weekend_mode) {
            preferences.click(R.string.active)
            preferences.click(R.string.days_string)
            dialogs.selectItem(frozenToday().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()))
            dialogs.confirm()
            preferences.click(R.string.weekend_start_time)
            pickers.pickTime(windowStart)
            preferences.click(R.string.weekend_end_time)
            pickers.pickTime(windowEnd)
        }

        seed.medicine(TEST_MED) { reminder("1", reminderTime) }

        navigation.toOverview()
        overview.assertEventContains(timeFormatter().minutesToTimeString(windowEnd.hour * 60 + windowEnd.minute))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun reschedule() {
        settings.click(R.string.display_settings, R.string.combine_notifications)

        seed.medicine(TEST_MED) { intervalReminder("1", intervalWithinToday()) }

        navigation.toOverview()
        overview.assertEventCountAtLeast(2)
        overview.longClickEvent(0)
        overview.clickEvent(1)

        overview.clickSelectionAction(R.string.reschedule_reminder)

        val rescheduleTime = laterToday()
        pickers.pickTime(rescheduleTime)

        val timeString = timeFormatter().minutesToTimeString(rescheduleTime.hour * 60 + rescheduleTime.minute)
        overview.assertEventState(0, R.string.please_wait)
        overview.assertEventTextContains(0, timeString)

        overview.assertEventState(1, R.string.please_wait)
        overview.assertEventTextContains(1, timeString)

        overview.longClickEvent(0)
        overview.assertSelectionCount(2)
        overview.clickSelectionAction(R.string.taken)

        overview.assertEventState(0, R.string.taken)
        overview.assertEventTextContains(0, timeString)

        overview.assertEventState(1, R.string.taken)
        overview.assertEventTextContains(1, timeString)
    }

    private class CyclicReminderInfo(
        val consecutiveDays: Int,
        val pauseDays: Int,
        val shouldHaveInfo: Boolean
    )
}
