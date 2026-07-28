package com.futsch1.medtimer

import androidx.test.espresso.Espresso
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test


class CalendarTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun calendarTest() {
        menus.clickAppOption(R.string.generate_test_data)

        overview.take(GINSENG)

        navigation.toMedicines()

        medicines.clickItem(0)
        menus.clickEditMedicineOption(R.string.calendar)
        assertContains(com.futsch1.medtimer.feature.ui.R.id.currentDayEvents, OMEGA_3)
        Espresso.pressBack()

        navigation.toAnalysis()
        statistics.selectView(StatisticFragment.CALENDAR)
        statistics.assertCalendarDayEventsContain(OMEGA_3)
        statistics.assertCalendarDayEventsContain(GINSENG)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun testDeletedEventNotInCalendarView() {
        overview.logManualDose()
        clickDialogItem(R.string.custom)
        writeTo(android.R.id.input, "Test")
        clickDialogPositiveButton(false)
        clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        overview.assertEventContains("Test")

        overview.clickEventState(0)
        overview.clickAction(R.string.delete)
        clickDialogPositiveButton()
        overview.assertEventCount(0)

        navigation.toAnalysis()
        statistics.selectView(StatisticFragment.CALENDAR)
        statistics.assertNoCalendarDayEventContains("Test")
    }

    companion object {
        private const val OMEGA_3 = "Omega 3 (EPA/DHA 500mg)"
        private const val GINSENG = "Ginseng (200mg)"
    }
}
