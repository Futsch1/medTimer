package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import kotlin.time.Duration.Companion.hours

const val TEST_MED_1 = "Test"
const val TEST_MED_2 = "Test2"
const val TEST_MED_3 = "A test"

@HiltAndroidTest
class MedicineHandlingTest : MedTimerTestBase() {

    @Test
    @AllowFlaky(attempts = 3)
    fun medicineMoveTest() {
        medicines.create(TEST_MED_1)
        medicineEditor.assertTitle(TEST_MED_1)
        medicines.create(TEST_MED_2)
        medicineEditor.assertTitle(TEST_MED_2)

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
        settings.inSection(R.string.notification_reminder_settings) {
            preferences.click(R.string.dismiss_notification_action)
            dialogs.clickItem(R.string.snooze)
        }

        seed.medicine(TEST_MED_1) {
            cannotBeSkipped()
            intervalReminder("1", 2.hours)
        }

        notifications.inShade {
            assertShows(TEST_MED_1)
            assertShowsAction(R.string.taken)
            assertNoAction(R.string.snooze)
            assertNoAction(R.string.skipped)
            dismiss(TEST_MED_1)
        }

        navigation.toOverview()
        overview.clickEventState(0)
        overview.assertActionAbsent(R.string.skipped)
        overview.closeActionMenu()
        overview.clickEventState(1)
        overview.assertActionAbsent(R.string.skipped)
        overview.closeActionMenu()
    }


    @Test
    @AllowFlaky(attempts = 3)
    fun medicineCannotBeSkippedPreferenceTest() {
        settings.inSection(R.string.notification_reminder_settings) {
            preferences.click(R.string.reminders_cannot_be_skipped)
        }

        seed.medicine(TEST_MED_1) { intervalReminder("1", 2.hours) }

        notifications.inShade {
            assertShows(TEST_MED_1)
            assertShowsAction(R.string.taken)
            assertNoAction(R.string.snooze)
            assertNoAction(R.string.skipped)
            dismiss(TEST_MED_1)
        }

        navigation.toOverview()
        overview.clickEventState(0)
        overview.assertActionAbsent(R.string.skipped)
        overview.closeActionMenu()
        overview.clickEventState(1)
        overview.assertActionAbsent(R.string.skipped)
        overview.closeActionMenu()
    }
}
