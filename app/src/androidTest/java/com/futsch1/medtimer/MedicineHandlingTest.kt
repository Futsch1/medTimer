package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

const val TEST_MED_1 = "Test"
const val TEST_MED_2 = "Test2"
const val TEST_MED_3 = "A test"

@HiltAndroidTest
class MedicineHandlingTest : MedTimerTestBase() {

    @Test
    @AllowFlaky(attempts = 3)
    fun medicineMoveTest() {
        medicines.create(TEST_MED_1)
        medicines.create(TEST_MED_2)

        medicines.assertCount(2)
        medicines.assertAtPosition(0, TEST_MED_1)
        medicines.assertAtPosition(1, TEST_MED_2)

        medicines.dragItem(0, 1)
        medicines.assertAtPosition(0, TEST_MED_2)
        medicines.clickItem(0)
        medicineEditor.rename(TEST_MED_2 + "_")
        medicines.assertAtPosition(0, TEST_MED_2 + '_')

        medicines.dragItem(1, 0)
        medicines.assertAtPosition(0, TEST_MED_1)
        medicines.dragItem(0, 1)

        medicines.create(TEST_MED_3)

        medicines.assertCount(3)
        medicines.assertAtPosition(2, TEST_MED_3)

        menus.clickMedicinesOption(R.string.by_name)
        medicines.assertAtPosition(0, TEST_MED_3)
        medicines.assertAtPosition(1, TEST_MED_1)
        medicines.assertAtPosition(2, TEST_MED_2 + '_')
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
