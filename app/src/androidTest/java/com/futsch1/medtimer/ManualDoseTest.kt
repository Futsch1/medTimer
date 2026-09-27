package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

private const val TEST_13_ = "Test (13)"
private const val GINSENG = "Ginseng (200mg)"

@HiltAndroidTest
class ManualDoseTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 1)
    fun testManualDose() {
        menus.clickAppOption(R.string.generate_test_data)

        navigation.toOverview()

        manualDose.log(GINSENG, amount = "12")

        overview.assertEventContains("Ginseng (200mg)")

        manualDose.logCustom("Test")
        overview.assertEventContains("Test")

        manualDose.inPicker {
            selectMedicine("Test")
            enterAmount("13")
            confirmTime()
        }

        overview.assertEventContains(TEST_13_)

        manualDose.inPicker {
            selectMedicine("Test")
            assertAmountPrefilled("13")
            cancel()
        }

        navigation.toMedicines()
    }

    @Test
    @AllowFlaky(attempts = 1)
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
}
