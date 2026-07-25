package com.futsch1.medtimer

import androidx.test.espresso.Espresso
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.ui.AppOptionsTestTags
import com.futsch1.medtimer.feature.ui.overview.OverviewTestTags


class CalendarTest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 3)
    fun calendarTest() {
        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.GENERATE_TEST_DATA)

        clickOverviewEventState(0)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.taken)
        clickOverviewEventState(2)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.taken)
        clickOverviewEventState(3)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.taken)
        clickOverviewEventState(4)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.taken)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        AndroidTestHelper.clickMedicineItem(0)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.calendar)
        assertContains(com.futsch1.medtimer.feature.ui.R.id.currentDayEvents, "Omega 3 (EPA/DHA 500mg)")
        Espresso.pressBack()

        navigateTo(AndroidTestHelper.MainMenu.ANALYSIS)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // View chips are icon-only; labels are exposed as content descriptions.
        device.findObject(By.desc(context.getString(R.string.calendar)))?.click()
        AndroidTestHelper.waitForIdle(500)
        // The Compose calendar pre-selects today, so today's events render without tapping a cell. Assert a
        // medicine from the top of today's (long) list, which stays on screen — the panel scrolls for the
        // rest, but UiAutomator only matches on-screen nodes.
        internalAssert(device.findObject(By.textContains("Omega 3 (EPA/DHA 500mg)")) != null)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun testDeletedEventNotInCalendarView() {
        // Create event
        clickTag(OverviewTestTags.LOG_MANUAL_DOSE)
        clickOn(R.string.custom)
        writeTo(android.R.id.input, "Test")
        clickDialogPositiveButton(false)
        clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        // Delete event
        clickOverviewEventState(0)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.delete)
        clickDialogPositiveButton()

        // Check that the event is not listed in the calendar view
        navigateTo(AndroidTestHelper.MainMenu.ANALYSIS)
        val device2 = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context2 = InstrumentationRegistry.getInstrumentation().targetContext
        device2.findObject(By.desc(context2.getString(R.string.calendar)))?.click()
        AndroidTestHelper.waitForIdle(500)
        // The deleted event has no day with events to reveal, so its text is absent entirely.
        internalAssert(device2.findObject(By.textContains("Test")) == null)
    }
}
