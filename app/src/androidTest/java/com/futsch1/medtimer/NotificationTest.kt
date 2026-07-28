package com.futsch1.medtimer

import android.app.Activity
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver
import com.futsch1.medtimer.feature.reminders.alarm.ReminderAlarmActivity
import com.futsch1.medtimer.feature.ui.AppOptionsTestTags
import com.futsch1.medtimer.utilities.pollUntil
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertTrue


const val TEST_MED = "Test med"
private const val ALARM_CLOSE_TIMEOUT = 10_000L

private const val SECOND_ONE = "second one"
private const val FIRST_REMINDER = "First reminder"
private const val SECOND_REMINDER = "Second reminder"
private const val TEST_VARIABLE_AMOUNT = "Test variable amount"
private const val TEST_ANOTHER_VARIABLE_AMOUNT = "Test another variable amount"


class NotificationTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun notificationTest() {
        medicines.create(TEST_MED)

        // Set color and icon
        openEditMedicineMenu()
        clickMenuItem(R.string.medicine_settings)
        clickPreference(R.string.color)
        clickPreference(R.string.select_color)
        onView(withResourceName("hexEdit")).perform(
            ViewActions.clearText(),
            ViewActions.typeText("deadbe")
        )
        closeSoftKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.confirmSelectColor)
        pressBack()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.selectIcon)
        onView(withResourceName("icd_rcv_icon_list")).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1,
                click()
            )
        )

        medicineEditor.addReminder(
            "1",
            aboutToFire()
        )

        medicineEditor.openAdvancedSettings()

        clickPreference(R.string.add_linked_reminder)
        clickDialogPositiveButton()
        pickers.pickDuration(0, 1)

        navigation.toOverview()

        baristaRule.activityTestRule.finishActivity()

        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(InstrumentationRegistry.getInstrumentation().targetContext)
        awaitAndDismissNotification()
        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(InstrumentationRegistry.getInstrumentation().targetContext)
        awaitAndDismissNotification()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun actionOnDismissedNotification() {
        // Skip reminder on dismiss
        settings.inSection(R.string.notification_reminder_settings) {
            clickPreference(R.string.dismiss_notification_action)
            clickDialogItem(R.string.skip_reminder)
        }

        navigation.toMedicines()

        medicines.create(TEST_MED)
        // Interval reminder (amount 1) 2 hours from now
        medicineEditor.addIntervalReminder("1", 120)
        pressBack()

        navigation.toAnalysis()
        awaitAndDismissNotification()

        // Check overview and next reminders
        navigation.toOverview()
        overview.assertEventState(0, R.string.skipped)

        // Now change to action taken on dismiss
        settings.inSection(R.string.notification_reminder_settings) {
            clickPreference(R.string.dismiss_notification_action)
            clickDialogItem(R.string.taken)
        }

        // Clear event data (causes reminder to be re-raised)
        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.CLEAR_EVENTS)
        clickDialogPositiveButton()

        navigation.toAnalysis()
        awaitAndDismissNotification()

        // Check overview and next reminders
        navigation.toOverview()
        overview.assertEventState(0, R.string.taken)
    }

    @Test
    fun repeatingReminders() {
        // Repeat reminder every minute and enable exact reminders
        settings.inSection(R.string.notification_reminder_settings) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                clickPreference(R.string.exact_reminders)
            }
            clickPreference(R.string.repeat_reminders)
            onView(
                allOf(
                    withText(R.string.repeat_reminders),
                    withResourceName("title")
                )
            ).perform(click())
            clickPreference(R.string.time_between_repetitions)
            clickDialogItem(R.string.minutes_1)
            pressBack()
        }

        settings.click(R.string.display_settings, R.string.combine_notifications)

        medicines.create(TEST_MED)
        medicineEditor.addReminder(FIRST_REMINDER, LocalTime.of(22, 0))
        medicineEditor.addReminder(SECOND_REMINDER, LocalTime.of(22, 0))
        pressBack()

        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(
            InstrumentationRegistry.getInstrumentation().targetContext,
            0
        )

        notifications.inShade {
            assertShows(TEST_MED)
            assertShows(FIRST_REMINDER)
            assertShows(SECOND_REMINDER)
        }
        navigation.toOverview()

        overview.clickEventState(0)
        clickMenuItem(R.string.taken)
        navigation.toAnalysis()

        notifications.inShade {
            assertHidden(FIRST_REMINDER)
            val text = assertShows(SECOND_REMINDER).text

            awaitGone(text, timeoutMillis = 120_000)
            assertShows(TEST_MED)
            assertHidden(FIRST_REMINDER)
            assertShows(SECOND_REMINDER)
        }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun variableAmount() {
        settings.click(R.string.display_settings, R.string.combine_notifications)

        medicines.create(TEST_MED)
        medicineEditor.addReminder("1", LocalTime.of(20, 0))
        medicineEditor.openAdvancedSettings()
        clickPreference(R.string.variable_amount)
        pressBack()
        openEditMedicineMenu()
        clickMenuItem(R.string.duplicate_including_reminders)

        medicines.clickItem(1)
        medicineEditor.rename(SECOND_ONE)
        pressBack()

        notifications.inShade {
            ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(
                InstrumentationRegistry.getInstrumentation().targetContext,
                0
            )
            assertShows(TEST_MED)
            clickAction(R.string.taken)
        }

        awaitInputDialog()
        assertDialogContains(TEST_MED)
        writeTo(android.R.id.input, TEST_VARIABLE_AMOUNT)
        clickDialogPositiveButton()

        awaitInputDialog()
        assertDialogContains(SECOND_ONE)
        writeTo(android.R.id.input, TEST_ANOTHER_VARIABLE_AMOUNT)
        clickDialogPositiveButton()

        navigation.toOverview()
        overview.assertEventContains(TEST_VARIABLE_AMOUNT)
        overview.assertEventContains(TEST_ANOTHER_VARIABLE_AMOUNT)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun variableAmountBigButton() {
        settings.inSection(R.string.display_settings) {
            clickPreference(R.string.big_notifications)
            clickPreference(R.string.combine_notifications)
        }

        medicines.create(TEST_MED)
        medicineEditor.addReminder("1", LocalTime.of(20, 0))
        medicineEditor.openAdvancedSettings()
        clickPreference(R.string.variable_amount)
        pressBack()
        openEditMedicineMenu()
        clickMenuItem(R.string.duplicate_including_reminders)
        medicines.clickItem(0)
        medicineEditor.addReminder("Not variable", LocalTime.of(20, 0))
        pressBack()

        medicines.clickItem(1)
        medicineEditor.rename(SECOND_ONE)
        pressBack()

        navigation.toAnalysis()

        notifications.inShade {
            ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(
                InstrumentationRegistry.getInstrumentation().targetContext,
                0
            )
            assertShows(TEST_MED)
            clickAction(R.string.taken)
        }

        awaitInputDialog()
        assertDialogContains(TEST_MED)
        writeTo(android.R.id.input, TEST_VARIABLE_AMOUNT)
        clickDialogPositiveButton()

        awaitInputDialog()
        assertDialogContains(SECOND_ONE)
        writeTo(android.R.id.input, TEST_ANOTHER_VARIABLE_AMOUNT)
        clickDialogPositiveButton()

        navigation.toOverview()
        overview.assertEventContains(TEST_VARIABLE_AMOUNT)
        overview.assertEventContains(TEST_ANOTHER_VARIABLE_AMOUNT)
        overview.assertEventContains("Not variable")

        overview.clickDay(LocalDate.now().plusDays(1))
        overview.clickEventState(0)
        clickMenuItem(R.string.taken)
        writeTo(android.R.id.input, "Test variable amount again")
        clickDialogPositiveButton()

        overview.assertEventContains("Test variable amount again")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun customSnooze() {
        settings.inSection(R.string.snooze_settings) {
            clickPreference(R.string.snooze_duration)
            clickDialogItem(R.string.custom)
        }
        settings.inSection(R.string.notification_reminder_settings) {
            clickPreference(R.string.dismiss_notification_action)
            clickDialogItem(R.string.snooze)
        }

        medicines.create(TEST_MED)
        medicineEditor.addIntervalReminder("1", 120)

        navigation.toAnalysis()
        notifications.inShade {
            assertShows(TEST_MED)
            dismiss(TEST_MED)
        }

        awaitInputDialog()
        writeTo(android.R.id.input, "5")
        clickDialogPositiveButton()

        navigation.toOverview()

        overview.assertEventState(0, R.string.reminded)

        navigation.toAnalysis()

        settings.inSection(R.string.notification_reminder_settings) {
            clickPreference(R.string.dismiss_notification_action)
            clickDialogItem(R.string.taken)
        }
        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(InstrumentationRegistry.getInstrumentation().targetContext)
        notifications.inShade {
            awaitShade()
            assertShows(TEST_MED)
            assertShowsAction(R.string.skipped)
            assertNoAction(R.string.taken)
            assertShowsAction(R.string.snooze)
            clickAction(R.string.snooze)
        }

        awaitInputDialog()
        writeTo(android.R.id.input, "13")
        clickDialogPositiveButton()

        navigation.toOverview()
        overview.assertEventState(1, R.string.reminded)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun hiddenMedicineName() {
        settings.click(R.string.privacy_settings, R.string.hide_med_name)

        medicines.create(TEST_MED)
        medicineEditor.addIntervalReminder("1", 120)
        pressBack()
        medicines.assertNameContains(TEST_MED)

        notifications.inShade { assertShows("T*******") }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun bigButtons() {
        settings.click(R.string.display_settings, R.string.big_notifications)

        medicines.create(TEST_MED)
        medicineEditor.addIntervalReminder("1", 120)
        pressBack()

        notifications.inShade {
            assertShows(TEST_MED)
            expandFor(actionLabel(R.string.taken))
            assertShowsButton("takenButton")
            assertShowsButton("skippedButton")
            assertShowsButton("snoozeButton")
        }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun sameTimeReminders() {
        settings.click(R.string.display_settings, R.string.combine_notifications)

        medicines.create(TEST_MED)
        val notificationTime = aboutToFire()

        medicineEditor.addReminder(
            "1",
            notificationTime
        )
        medicineEditor.addReminder(
            SECOND_ONE,
            notificationTime
        )

        notifications.inShade {
            ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(
                InstrumentationRegistry.getInstrumentation().targetContext,
                0
            )
            assertShows(TEST_MED)
            assertShows(SECOND_ONE)
            clickAction(R.string.taken)
        }

        navigation.toOverview()
        overview.assertEventState(0, R.string.taken)
        overview.assertEventState(1, R.string.taken)

        overview.longClickEvent(0)
        overview.clickSelectionAction(R.string.skipped)

        overview.assertEventState(0, R.string.skipped)
        overview.assertEventState(1, R.string.skipped)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun automaticallyTakenTest() {
        medicines.create(TEST_MED)
        val notificationTime = aboutToFire()

        medicineEditor.addReminder("1", notificationTime)
        medicineEditor.openAdvancedSettings()
        clickPreference(R.string.automatically_taken)
        pressBack()

        navigation.toOverview()
        overview.assertEventState(0, R.string.please_wait)

        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(InstrumentationRegistry.getInstrumentation().targetContext)
        overview.assertEventState(0, R.string.taken)

        notifications.inShade { assertHidden(actionLabel(R.string.taken)) }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun alarmTest() {
        val timeToNotify = 10_000L
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        device.wakeUp()

        medicines.create(TEST_MED)
        medicineEditor.addIntervalReminder("1", 2)
        openEditMedicineMenu()
        clickMenuItem(R.string.medicine_settings)
        clickOn(R.string.notification_importance)
        clickOn(R.string.high_and_alarm)
        pressBack()

        navigation.toOverview()
        overview.clickEventState(0)
        clickMenuItem(R.string.taken)

        device.sleep()

        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(context, timeToNotify, 0)
        takeOnAlarmScreen(timeToNotify * 4, "Alarm screen did not appear")

        overview.assertEventState(1, R.string.taken)

        device.sleep()

        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(context, timeToNotify, 0)
        awaitAlarmScreen(timeToNotify * 4, "Alarm screen did not appear a second time")
        ReminderProcessorBroadcastReceiver.requestScheduleNowForTests(context)
        takeOnAlarmScreen(timeToNotify * 4, "Alarm screen did not appear a second time")

        overview.assertEventState(2, R.string.taken)
        overview.assertEventState(3, R.string.reminded)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun scheduleReminderTest() {
        medicines.create(TEST_MED)

        medicineEditor.addReminder("1", laterToday())

        navigation.toOverview()

        overview.clickEventState(0)
        clickMenuItem(R.string.reschedule_reminder)

        pickers.pickTime(LocalTime.of(4, 0))

        notifications.inShade { assertShows(TEST_MED) }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun noSkippedButton() {
        medicines.create(TEST_MED)
        openEditMedicineMenu()
        clickMenuItem(R.string.medicine_settings)
        clickOn(R.string.medicine_cannot_be_skipped)
        pressBack()
        medicineEditor.addIntervalReminder("1", 120)
        pressBack()

        notifications.inShade {
            assertShows(TEST_MED)
            assertShowsAction(R.string.taken)
            assertShowsAction(R.string.snooze)
            assertNoAction(R.string.skipped)
            dismiss(TEST_MED)
        }

        navigation.toOverview()
        overview.assertEventState(0, R.string.reminded)

        settings.click(R.string.display_settings, R.string.big_notifications)

        navigation.toMedicines()

        medicines.create(TEST_MED_2)
        openEditMedicineMenu()
        clickMenuItem(R.string.medicine_settings)
        clickOn(R.string.medicine_cannot_be_skipped)
        pressBack()
        medicineEditor.addIntervalReminder("1", 120)

        notifications.inShade {
            assertShows(TEST_MED_2)
            expandFor(actionLabel(R.string.taken))
            assertShowsButton("takenButton")
            assertNoButton("skippedButton")
            assertShowsButton("snoozeButton")
        }
    }

    /** The alarm screen is MedTimer's own activity, so Espresso drives it once it has resumed. */
    private fun awaitAlarmScreen(timeoutMillis: Long, message: String) {
        assertTrue(pollUntil(timeoutMillis) { alarmActivity() != null }, message)
    }

    private fun takeOnAlarmScreen(timeoutMillis: Long, message: String) {
        awaitAlarmScreen(timeoutMillis, message)
        assertTrue(
            pollUntil(ALARM_CLOSE_TIMEOUT) {
                clickTakenOnAlarmScreen()
                alarmActivity() == null
            },
            "Alarm screen did not close"
        )
    }

    private fun clickTakenOnAlarmScreen() {
        val activity = alarmActivity() ?: return
        onView(withId(com.futsch1.medtimer.feature.reminders.R.id.takenButton))
            .inRoot(RootMatchers.withDecorView(`is`(activity.window.decorView)))
            .withFailureHandler { _, _ -> }
            .perform(click())
    }

    private fun alarmActivity(): Activity? {
        var activity: Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull { it is ReminderAlarmActivity }
        }
        return activity
    }

    private fun awaitAndDismissNotification() {
        notifications.inShade {
            assertShows(TEST_MED)
            dismiss(TEST_MED)
        }
    }
}
