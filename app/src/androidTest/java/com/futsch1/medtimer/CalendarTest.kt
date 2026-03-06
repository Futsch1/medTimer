package com.futsch1.medtimer


import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.futsch1.medtimer.statistics.ui.StatisticsTestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class CalendarTest : BaseTestHelper() {
    @Test
    fun calendarTest() {
        openMenu()
        clickOn(R.string.generate_test_data)

        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)
        clickListItemChild(R.id.reminders, 2, R.id.stateButton)
        clickOn(R.id.takenButton)
        clickListItemChild(R.id.reminders, 3, R.id.stateButton)
        clickOn(R.id.takenButton)
        clickListItemChild(R.id.reminders, 4, R.id.stateButton)
        clickOn(R.id.takenButton)

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        clickListItem(R.id.medicineList, 0)
        clickOn(R.id.openCalendar)
        composeTestRule.onNodeWithText("Omega 3 (EPA/DHA 500mg)").assertExists()
        pressBack()

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS)

        composeTestRule.onNodeWithTag(StatisticsTestTags.CALENDAR_CHIP).performClick()
        composeTestRule.onNodeWithText("Selen (200 µg)").assertExists()
    }

    @Test
    fun testDeletedEventNotInCalendarView() {
        // Create event
        clickOn(R.id.logManualDose)
        clickOn(R.string.custom)
        writeTo(android.R.id.input, "Test")
        clickDialogPositiveButton()
        clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        // Delete event
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.deleteButton)
        clickDialogPositiveButton()

        // Check that the event is not listed in the calendar view
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS)
        composeTestRule.onNodeWithTag(StatisticsTestTags.CALENDAR_CHIP).performClick()
        composeTestRule.onNodeWithText("Test").assertDoesNotExist()
    }
}
