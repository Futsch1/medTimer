package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.core.ui.R
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import java.text.DateFormat
import java.time.LocalDate
import java.util.Calendar
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours


@HiltAndroidTest
class BasicUITest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun basicUITest() {
        medicines.create(" Test ")
        medicineEditor.addReminder("1", laterToday())

        reminders.inSettingsOf(0) {
            inDosingInstructions {
                useSampleInstruction(R.string.before_meal)
                assertDosingInstructions(getString(R.string.before_meal))
            }
        }

        reminders.inSettingsOf(0) {
            assertDosingInstructions(getString(R.string.before_meal))
        }

        medicineEditor.setAmount(" 2 ")

        medicines.clickItem(0)
        medicineEditor.assertAmount("2")
        reminders.duplicate(0)

        reminders.assertCount(2)

        navigation.toOverview()
        overview.assertEventContains(TEST_2)

        navigation.toMedicines()
        medicines.clickItem(0)
        medicineEditor.rename(" Test2 ")

        navigation.toOverview()
        overview.assertEventContains("Test2 (2)")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun menuHandlingTest() {
        seed.medicine("Test") { reminder("1", laterToday()) }

        medicines.clickItem(0)

        val cycleStart = Calendar.getInstance()
        cycleStart.set(2025, 1, 1)
        val cycleStartString =
            DateFormat.getDateInstance(DateFormat.SHORT).format(cycleStart.getTime())

        reminders.inSettingsOf(0) {
            inCycle {
                setCycleStartDate(cycleStart.time)
                setConsecutiveDays(5)
                setPauseDays(6)
            }

            inWeekdays {
                clickItem(R.string.monday)
                clickItem(R.string.tuesday)
            }

            inDaysOfMonth {
                clickItem("1")
                clickItem("3")
            }
        }

        reminders.inSettingsOf(0) {
            inCycle {
                assertCycleStartDate(cycleStartString)
                assertConsecutiveDays("5")
                assertPauseDays("6")
            }

            inWeekdays {
                assertItemNotChecked(R.string.monday)
                assertItemNotChecked(R.string.tuesday)
                assertItemChecked(R.string.wednesday)
            }

            inDaysOfMonth {
                assertItemChecked("1")
                assertItemNotChecked("2")
                assertItemChecked("3")
            }
        }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun notesTest() {
        seed.medicine("Test")

        medicines.clickItem(0)

        val notesText = "Contains catnip\n\nmeow :3"

        notes.save(notesText)

        medicines.clickItem(0)
        notes.assertContains(notesText)

        notes.clearAndDiscard()
        notes.assertContains(notesText)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun appIntro() {
        appIntro.show()
        appIntro.assertPage(R.string.intro_welcome, R.string.intro_welcome_description)
        appIntro.skip()

        navigation.assertOverviewShown()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun overviewFilters() {
        val medicineId = seed.medicine("Test") { intervalReminder("2", 24.hours) }

        navigation.toOverview()
        overview.assertEventContains(TEST_2)

        overview.toggleFilter(OverviewFilter.RAISED)
        overview.assertEventContains(TEST_2)
        overview.toggleFilter(OverviewFilter.RAISED)

        overview.assertEventContains(TEST_2)

        overview.toggleFilter(OverviewFilter.TAKEN)
        overview.assertEventCount(0)
        overview.toggleFilter(OverviewFilter.TAKEN)

        overview.assertEventContains(TEST_2)

        overview.clickEventState(0)
        overview.clickAction(R.string.taken)

        overview.toggleFilter(OverviewFilter.TAKEN)
        overview.assertEventContains(TEST_2)
        overview.toggleFilter(OverviewFilter.TAKEN)

        overview.assertEventContains(TEST_2)

        overview.toggleFilter(OverviewFilter.RAISED)
        overview.assertEventCount(0)
        overview.toggleFilter(OverviewFilter.RAISED)

        overview.toggleFilter(OverviewFilter.SKIPPED)
        overview.assertEventCount(0)
        overview.toggleFilter(OverviewFilter.SKIPPED)

        overview.toggleFilter(OverviewFilter.SCHEDULED)
        overview.assertEventCount(0)
        overview.toggleFilter(OverviewFilter.SCHEDULED)

        seed.remindersOf(medicineId) { reminder("1", laterToday()) }

        navigation.toOverview()

        overview.assertEventContains("Test (1)")

        overview.toggleFilter(OverviewFilter.SCHEDULED)
        overview.assertEventContains("Test (1)")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun overviewDaySelection() {
        menus.clickAppOption(R.string.generate_test_data_and_events)

        val today = LocalDate.now()
        val secondDay = today.plusDays(1)

        overview.selectDay(secondDay)
        assertTrue(overview.eventCount() > 0)

        navigation.toOverview()
        overview.assertDaySelected(today)

        navigation.toMedicines()
        navigation.toOverview()
        overview.assertDaySelected(today)
        assertTrue(overview.eventCount() > 0)

        overview.nextWeek()
        assertTrue(overview.eventCount() > 0)

        overview.previousWeek()
        overview.assertDaySelected(today)
        assertTrue(overview.eventCount() > 0)

        overview.previousWeek()
        assertTrue(overview.eventCount() > 0)
    }

    companion object {
        const val TEST_2: String = "Test (2)"
    }
}
