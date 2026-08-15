package com.futsch1.medtimer

import android.os.Build
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.utilities.awaitNextSecond
import com.futsch1.medtimer.utilities.fireNextAlarmsAfter
import com.futsch1.medtimer.utilities.scheduleRemindersNow
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


const val TEST_MED = "Test med"

private const val SECOND_ONE = "second one"
private const val FIRST_REMINDER = "First reminder"
private const val SECOND_REMINDER = "Second reminder"
private const val TEST_VARIABLE_AMOUNT = "Test variable amount"
private const val TEST_ANOTHER_VARIABLE_AMOUNT = "Test another variable amount"
private const val REPEAT_AFTER_MILLIS = 5_000L

/** Covers the repeat whether the armed alarm pulled it forward or it stayed at the configured minute. */
private const val REPEAT_TIMEOUT_MILLIS = 90_000L


@HiltAndroidTest
class NotificationTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun notificationTest() {
        seed.medicine(TEST_MED) {
            reminder("1", aboutToFire())
            linkedReminder("1", after = 1.minutes)
        }

        medicines.clickItem(0)
        medicineSettings.setColorAndIcon(hex = "deadbe", iconPosition = 1)

        navigation.toOverview()

        baristaRule.activityTestRule.finishActivity()

        scheduleRemindersNow()
        awaitAndDismissNotification()
        scheduleRemindersNow()
        awaitAndDismissNotification()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun actionOnDismissedNotification() {
        // Skip reminder on dismiss
        settings.inSection(R.string.notification_reminder_settings) {
            preferences.click(R.string.dismiss_notification_action)
            dialogs.clickItem(R.string.skip_reminder)
        }

        // Interval reminder (amount 1) 2 hours from now
        seed.medicine(TEST_MED) { intervalReminder("1", 2.hours) }

        navigation.toAnalysis()
        awaitAndDismissNotification()

        // Check overview and next reminders
        navigation.toOverview()
        overview.assertEventState(0, R.string.skipped)

        // Now change to action taken on dismiss
        settings.inSection(R.string.notification_reminder_settings) {
            preferences.click(R.string.dismiss_notification_action)
            dialogs.clickItem(R.string.taken)
        }

        // Clear event data (causes reminder to be re-raised)
        menus.clickAppOption(R.string.clear_events)
        dialogs.confirm()

        navigation.toAnalysis()
        awaitAndDismissNotification()

        // Check overview and next reminders
        navigation.toOverview()
        overview.assertEventState(0, R.string.taken)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun repeatingReminders() {
        // Repeat reminder every minute and enable exact reminders
        settings.inSection(R.string.notification_reminder_settings) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                preferences.click(R.string.exact_reminders)
            }
            // The first tap turns the switch on, the second opens the screen behind it.
            preferences.click(R.string.repeat_reminders)
            preferences.click(R.string.repeat_reminders)
            preferences.click(R.string.time_between_repetitions)
            dialogs.clickItem(R.string.minutes_1)
            preferences.back()
        }

        settings.click(R.string.display_settings, R.string.combine_notifications)

        seed.medicine(TEST_MED) {
            reminder(FIRST_REMINDER, laterToday())
            reminder(SECOND_REMINDER, laterToday())
        }

        scheduleRemindersNow()

        notifications.inShade {
            assertShows(TEST_MED)
            assertShows(FIRST_REMINDER)
            assertShows(SECOND_REMINDER)
        }
        navigation.toOverview()

        // Read before the repeat is armed, after which it can land at any moment.
        val firstRaise = notifications.postedId(SECOND_REMINDER)

        fireNextAlarmsAfter(REPEAT_AFTER_MILLIS)
        overview.clickEventState(0)
        overview.clickAction(R.string.taken)
        navigation.toAnalysis()

        notifications.awaitRaisedAgain(SECOND_REMINDER, firstRaise, timeoutMillis = REPEAT_TIMEOUT_MILLIS)

        notifications.inShade {
            assertShows(TEST_MED)
            assertHidden(FIRST_REMINDER)
            assertShows(SECOND_REMINDER)
        }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun variableAmount() {
        settings.click(R.string.display_settings, R.string.combine_notifications)

        seed.medicine(TEST_MED) { reminder("1", laterToday()) }

        medicines.clickItem(0)
        reminders.inSettingsOf(0) { toggleVariableAmount() }
        menus.clickEditMedicineOption(R.string.duplicate_including_reminders)

        medicines.clickItem(1)
        medicineEditor.rename(SECOND_ONE)

        navigation.toAnalysis()

        notifications.inShade {
            scheduleRemindersNow()
            assertShows(TEST_MED)
            clickAction(R.string.taken)
        }

        enterVariableAmount(TEST_MED, TEST_VARIABLE_AMOUNT)
        enterVariableAmount(SECOND_ONE, TEST_ANOTHER_VARIABLE_AMOUNT)

        navigation.toOverview()
        overview.assertEventContains(TEST_VARIABLE_AMOUNT)
        overview.assertEventContains(TEST_ANOTHER_VARIABLE_AMOUNT)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun variableAmountBigButton() {
        settings.inSection(R.string.display_settings) {
            preferences.click(R.string.big_notifications)
            preferences.click(R.string.combine_notifications)
        }

        val medicineId = seed.medicine(TEST_MED) { reminder("1", laterToday(), variableAmount = true) }

        medicines.clickItem(0)
        menus.clickEditMedicineOption(R.string.duplicate_including_reminders)
        seed.remindersOf(medicineId) { reminder("Not variable", laterToday()) }

        medicines.clickItem(1)
        medicineEditor.rename(SECOND_ONE)

        navigation.toAnalysis()

        notifications.inShade {
            scheduleRemindersNow()
            assertShows(TEST_MED)
            clickAction(R.string.taken)
        }

        enterVariableAmount(TEST_MED, TEST_VARIABLE_AMOUNT)
        enterVariableAmount(SECOND_ONE, TEST_ANOTHER_VARIABLE_AMOUNT)

        navigation.toOverview()
        overview.assertEventContains(TEST_VARIABLE_AMOUNT)
        overview.assertEventContains(TEST_ANOTHER_VARIABLE_AMOUNT)
        overview.assertEventContains("Not variable")

        overview.selectDay(LocalDate.now().plusDays(1))
        overview.clickEventState(0)
        overview.clickAction(R.string.taken)
        dialogs.enterTextAndConfirm("Test variable amount again")

        overview.assertEventContains("Test variable amount again")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun customSnooze() {
        settings.inSection(R.string.snooze_settings) {
            preferences.click(R.string.snooze_duration)
            dialogs.clickItem(R.string.custom)
        }
        settings.inSection(R.string.notification_reminder_settings) {
            preferences.click(R.string.dismiss_notification_action)
            dialogs.clickItem(R.string.snooze)
        }

        seed.medicine(TEST_MED) { intervalReminder("1", 2.hours) }

        navigation.toAnalysis()
        notifications.inShade {
            assertShows(TEST_MED)
            dismiss(TEST_MED)
        }

        dialogs.awaitInput()
        dialogs.enterTextAndConfirm("5")

        navigation.toOverview()

        overview.assertEventState(0, R.string.reminded)

        navigation.toAnalysis()

        settings.inSection(R.string.notification_reminder_settings) {
            preferences.click(R.string.dismiss_notification_action)
            dialogs.clickItem(R.string.taken)
        }
        scheduleRemindersNow()
        notifications.inShade {
            awaitShade()
            assertShows(TEST_MED)
            assertShowsAction(R.string.skipped)
            assertNoAction(R.string.taken)
            assertShowsAction(R.string.snooze)
            clickAction(R.string.snooze)
        }

        dialogs.awaitInput()
        dialogs.enterTextAndConfirm("13")

        navigation.toOverview()
        overview.assertEventState(1, R.string.reminded)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun hiddenMedicineName() {
        settings.click(R.string.privacy_settings, R.string.hide_med_name)

        seed.medicine(TEST_MED) { intervalReminder("1", 2.hours) }
        medicines.assertNameContains(TEST_MED)

        notifications.inShade { assertShows("T*******") }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun bigButtons() {
        settings.click(R.string.display_settings, R.string.big_notifications)

        seed.medicine(TEST_MED) { intervalReminder("1", 2.hours) }

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

        val notificationTime = aboutToFire()

        seed.medicine(TEST_MED) {
            reminder("1", notificationTime)
            reminder(SECOND_ONE, notificationTime)
        }

        notifications.inShade {
            scheduleRemindersNow()
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
        seed.medicine(TEST_MED) { reminder("1", aboutToFire()) }

        medicines.clickItem(0)
        reminders.inSettingsOf(0) { toggleAutomaticallyTaken() }

        navigation.toOverview()
        overview.assertEventState(0, R.string.please_wait)

        scheduleRemindersNow()
        overview.assertEventState(0, R.string.taken)

        notifications.inShade { assertHidden(actionLabel(R.string.taken)) }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun alarmTest() {
        val timeToNotify = 10_000L
        alarm.wakeDevice()

        seed.medicine(TEST_MED) { intervalReminder("1", 2.minutes) }

        medicines.clickItem(0)
        medicineSettings.inSettings { setNotificationImportance(R.string.high_and_alarm) }

        navigation.toOverview()
        overview.clickEventState(0)
        overview.clickAction(R.string.taken)

        alarm.sleepDevice()

        scheduleRemindersNow(timeToNotify)
        alarm.take(timeToNotify * 4, "Alarm screen did not appear")

        overview.assertEventState(1, R.string.taken)

        alarm.sleepDevice()

        scheduleRemindersNow(timeToNotify)
        alarm.awaitShown(timeToNotify * 4, "Alarm screen did not appear a second time")
        awaitNextSecond()
        scheduleRemindersNow()
        alarm.take(timeToNotify * 4, "Alarm screen did not appear a second time")

        overview.assertEventState(2, R.string.taken)
        overview.assertEventState(3, R.string.reminded)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun scheduleReminderTest() {
        seed.medicine(TEST_MED) { reminder("1", laterToday()) }

        navigation.toOverview()

        overview.clickEventState(0)
        overview.clickAction(R.string.reschedule_reminder)

        pickers.pickTime(earlierToday())

        notifications.inShade { assertShows(TEST_MED) }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun noSkippedButton() {
        seed.medicine(TEST_MED) {
            cannotBeSkipped()
            intervalReminder("1", 2.hours)
        }

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

        seed.medicine(TEST_MED_2) {
            cannotBeSkipped()
            intervalReminder("1", 2.hours)
        }

        notifications.inShade {
            assertShows(TEST_MED_2)
            expandFor(actionLabel(R.string.taken))
            assertShowsButton("takenButton")
            assertNoButton("skippedButton")
            assertShowsButton("snoozeButton")
        }
    }

    /** The amount dialog opens per medicine, in the order the notifications were raised. */
    private fun enterVariableAmount(medicineName: String, amount: String) {
        dialogs.awaitInput()
        dialogs.assertContains(medicineName)
        dialogs.enterTextAndConfirm(amount)
    }

    private fun awaitAndDismissNotification() {
        notifications.inShade {
            assertShows(TEST_MED)
            dismiss(TEST_MED)
        }
    }
}
