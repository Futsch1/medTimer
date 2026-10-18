package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.R
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test


@HiltAndroidTest
class CalendarTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun calendarShowsEventsAndOmitsDeletedOnes() {
        menus.clickAppOption(R.string.generate_test_data)

        overview.take(GINSENG)

        navigation.toMedicines()

        medicines.clickItem(0)
        calendar.assertDayEventsContain(OMEGA_3)

        navigation.toAnalysis()
        statistics.selectView(StatisticFragment.CALENDAR)
        statistics.assertCalendarDayEventsContain(OMEGA_3)
        statistics.assertCalendarDayEventsContain(GINSENG)

        navigation.toOverview()
        manualDose.logCustom("Temporary dose")
        overview.assertEventContains("Temporary dose")
        overview.clickEventStateContaining("Temporary dose")
        overview.clickAction(R.string.delete)
        dialogs.confirm()

        navigation.toAnalysis()
        statistics.selectView(StatisticFragment.CALENDAR)
        statistics.assertNoCalendarDayEventContains("Temporary dose")
    }

    companion object {
        private const val OMEGA_3 = "Omega 3 (EPA/DHA 500mg)"
        private const val GINSENG = "Ginseng (200mg)"
    }
}
