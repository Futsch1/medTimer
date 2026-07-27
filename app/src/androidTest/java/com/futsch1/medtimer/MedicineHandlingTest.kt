package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.assertMedicineAtPosition
import com.futsch1.medtimer.AndroidTestHelper.clickMedicineItem
import com.futsch1.medtimer.AndroidTestHelper.dragMedicineItem
import com.futsch1.medtimer.AndroidTestHelper.scrollDown
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.utilities.openNotification
import org.junit.Test

const val TEST_MED_1 = "Test"
const val TEST_MED_2 = "Test2"
const val TEST_MED_3 = "A test"

class MedicineHandlingTest : BaseTestHelper() {

    @Test
    @AllowFlaky(attempts = 3)
    fun medicineMoveTest() {
        AndroidTestHelper.createMedicine(TEST_MED_1)
        AndroidTestHelper.createMedicine(TEST_MED_2)

        pressBack()

        assertMedicineAtPosition(0, TEST_MED_1)

        dragMedicineItem(0, 1)
        assertMedicineAtPosition(0, TEST_MED_2)
        clickMedicineItem(0)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editMedicineName, TEST_MED_2 + "_")
        pressBack()
        assertMedicineAtPosition(0, TEST_MED_2 + '_')

        dragMedicineItem(1, 0)
        assertMedicineAtPosition(0, TEST_MED_1)
        dragMedicineItem(0, 1)

        AndroidTestHelper.createMedicine(TEST_MED_3)
        pressBack()

        assertMedicineAtPosition(2, TEST_MED_3)

        openMenu()
        clickOn(R.string.sort)
        clickOn(R.string.by_name)
        assertMedicineAtPosition(0, TEST_MED_3)
        assertMedicineAtPosition(1, TEST_MED_1)
        assertMedicineAtPosition(2, TEST_MED_2 + '_')
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun medicineCannotBeSkippedTest() {
        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.notification_reminder_settings)
        clickOn(R.string.dismiss_notification_action)
        clickOn(R.string.snooze)
        pressBack()
        pressBack()

        AndroidTestHelper.createMedicine(TEST_MED_1)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openMedicineSettings)
        clickOn(R.string.medicine_cannot_be_skipped)
        pressBack()

        AndroidTestHelper.createIntervalReminder("1", 60)

        pressBack()

        openNotification().use {
            assert(
                !clickNotificationButton(
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()),
                    getNotificationText(R.string.skipped)
                )
            )
            assert(
                !clickNotificationButton(
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()),
                    getNotificationText(R.string.snooze)
                )
            )
        }

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.R.id.stateButton
        )
        assertNotDisplayed(R.string.skipped)
        pressBack()

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminders,
            1,
            com.futsch1.medtimer.feature.ui.R.id.stateButton
        )
        assertNotDisplayed(R.string.skipped)
    }


    @Test
    @AllowFlaky(attempts = 3)
    fun medicineCannotBeSkippedPreferenceTest() {
        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.notification_reminder_settings)
        scrollDown()
        clickOn(R.string.reminders_cannot_be_skipped)
        pressBack()
        pressBack()

        AndroidTestHelper.createMedicine(TEST_MED_1)
        AndroidTestHelper.createIntervalReminder("1", 60)

        pressBack()

        openNotification().use {
            assert(
                !clickNotificationButton(
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()),
                    getNotificationText(R.string.skipped)
                )
            )
            assert(
                !clickNotificationButton(
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()),
                    getNotificationText(R.string.snooze)
                )
            )
        }

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.R.id.stateButton
        )
        assertNotDisplayed(R.string.skipped)
        pressBack()

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminders,
            1,
            com.futsch1.medtimer.feature.ui.R.id.stateButton
        )
        assertNotDisplayed(R.string.skipped)
    }
}
