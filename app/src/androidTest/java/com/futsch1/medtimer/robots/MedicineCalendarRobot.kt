package com.futsch1.medtimer.robots

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.futsch1.medtimer.core.ui.R as CoreUiR

/** A medicine's own calendar, opened from the edit-medicine menu. */
class MedicineCalendarRobot(private val menus: MenuRobot) {

    fun assertDayEventsContain(expected: String) = inCalendar {
        assertContains(com.futsch1.medtimer.feature.ui.R.id.currentDayEvents, expected)
    }

    /** Opening and closing it is the assertion: the calendar has crashed on some reminder types. */
    fun assertOpens() = inCalendar {}

    private fun inCalendar(block: () -> Unit) {
        menus.clickEditMedicineOption(CoreUiR.string.calendar)
        block()
        pressBack()
    }
}
