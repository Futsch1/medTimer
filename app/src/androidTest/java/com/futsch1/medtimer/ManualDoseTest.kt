package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.ui.R
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

private const val TEST_13_ = "Test (13)"
private const val GINSENG = "Ginseng (200mg)"

@HiltAndroidTest
class ManualDoseTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun testManualDose() {
        menus.clickAppOption(R.string.generate_test_data)

        navigation.toOverview()

        manualDose.inPicker {
            assertBlankCustomMedicineRejected()
            cancelPicker()
        }

        manualDose.log(GINSENG, amount = "12")

        overview.assertEventContains("Ginseng (200mg)")

        manualDose.logCustom("Test")
        overview.assertEventContains("Test")

        manualDose.inPicker {
            chooseCustom("Test")
            enterAmount("13")
            confirmTime()
        }

        overview.assertEventContains(TEST_13_)

        val doseCountBeforeQuickLog = runBlocking {
            reminderEventRepository.getAllWithoutDeleted().count {
                it.medicineName == "Test" && it.amount == "13" && it.status == ReminderEvent.ReminderStatus.TAKEN
            }
        }
        manualDose.inPicker {
            assertRecentDoseOffered("Test", "13")
            selectRecentDose("Test", "13")
        }
        val doseCountAfterQuickLog = runBlocking {
            reminderEventRepository.getAllWithoutDeleted().count {
                it.medicineName == "Test" && it.amount == "13" && it.status == ReminderEvent.ReminderStatus.TAKEN
            }
        }
        assertEquals(doseCountBeforeQuickLog + 1, doseCountAfterQuickLog)
        overview.assertEventContains(TEST_13_)

        navigation.toMedicines()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun manualDoseUsesSelectedOverviewDate() {
        val selectedDay = LocalDate.of(2025, 7, 30)
        seed.medicine("Test")
        navigation.toOverview()
        overview.selectDay(selectedDay)

        manualDose.logCustom("Test")

        val event = runBlocking {
            reminderEventRepository.getAllWithoutDeleted().single {
                it.medicineName == "Test" && it.status == ReminderEvent.ReminderStatus.TAKEN
            }
        }
        assertEquals(selectedDay, event.remindedTimestamp.atZone(ZoneId.systemDefault()).toLocalDate())
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun testManualDoseOfDisabledReminder() {
        // Create medication with a disabled reminder
        seed.medicine("Test") { reminder("1 pill", active = false) }

        // Create manual dose of the disabled reminder
        navigation.toOverview()
        manualDose.log("Test", "1 pill")

        // Check if the event is created properly
        eventEditor.forEvent(0) {
            assertNameDoesNotContain("Test (1 pill)")
            assertName("Test")
            assertAmount("1 pill")
        }

        // Check that re-raise is not shown
        overview.clickEventState(0)
        overview.assertActionAbsent(R.string.re_raise_event)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun searchMedicine() {
        listOf("Vitamin X 500 mg", "Omega 3", "Vitamin D", "Ginseng", "Magnesium").forEach { seed.medicine(it) }

        navigation.toOverview()

        manualDose.inPicker {
            search("vitamin")
            assertMedicinesOffered("Vitamin X 500 mg", "Vitamin D")
            selectMedicine("Vitamin D")
            enterAmount("1")
            confirmTime()
        }

        overview.assertEventContains("Vitamin D")
    }
}
