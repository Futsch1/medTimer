package com.futsch1.medtimer

import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.R
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test


@HiltAndroidTest
class CalendarTest : MedTimerTestBase() {
    @Test
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
        seed.manualDose(TEMPORARY_DOSE)
        overview.assertEventContains(TEMPORARY_DOSE)
        overview.clickEventStateContaining(TEMPORARY_DOSE)
        overview.clickAction(R.string.delete)
        dialogs.confirm()

        navigation.toAnalysis()
        statistics.selectView(StatisticFragment.CALENDAR)
        statistics.assertNoCalendarDayEventContains(TEMPORARY_DOSE)
    }

    companion object {
        private const val OMEGA_3 = "Omega 3 (EPA/DHA 500mg)"
        private const val GINSENG = "Ginseng (200mg)"
        private const val TEMPORARY_DOSE = "Temporary dose"
    }
}
