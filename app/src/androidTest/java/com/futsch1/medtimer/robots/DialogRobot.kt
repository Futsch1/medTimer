package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoActivityResumedException
import androidx.test.espresso.NoMatchingRootException
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.futsch1.medtimer.robots.DialogRobot.Companion.DIALOG_TIMEOUT
import com.futsch1.medtimer.utilities.pollUntil
import org.hamcrest.Matchers

/**
 * AlertDialogs, for the tests where the dialog itself is the subject. A dialog raised on the way to
 * an outcome belongs to the robot that opened it, not here.
 */
class DialogRobot(private val ui: ComposeUi) {

    fun confirm(retryIfStillVisible: Boolean = true) {
        pollUntil(DIALOG_TIMEOUT) { positiveButtonShown() }
        confirmIfVisible()
        if (retryIfStillVisible) {
            confirmIfVisible()
        }
    }

    /** Leaves the dialog without accepting it. */
    fun dismiss() = pressBack()

    fun enterText(text: String) = writeTo(android.R.id.input, text)

    fun enterTextAndConfirm(text: String) {
        enterText(text)
        confirm()
    }

    /**
     * The amount and snooze dialogs open from a notification action, so the app has still to come
     * back to the foreground; matching in the dialog root is what waits for that window.
     */
    fun awaitInput() {
        pollUntil(DIALOG_FROM_NOTIFICATION_TIMEOUT) { inputShown() }
        onView(ViewMatchers.withId(android.R.id.input))
            .inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    private fun inputShown(): Boolean = try {
        onView(ViewMatchers.withId(android.R.id.input))
            .inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        true
    } catch (_: NoMatchingRootException) {
        false
    } catch (_: NoMatchingViewException) {
        false
    }

    fun assertInputContains(text: String) =
        onView(ViewMatchers.withId(android.R.id.input))
            .check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString(text))))

    /** Asserts text inside the dialog window, rather than anywhere in the app. */
    fun assertContains(text: String) {
        onView(ViewMatchers.withText(Matchers.containsString(text)))
            .inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun clickItem(text: String) = item(text).perform(ViewActions.click())

    fun clickItem(@StringRes textRes: Int) = clickItem(ui.getString(textRes))

    fun assertItemDisplayed(text: String) =
        item(text).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    fun assertItemChecked(text: String) =
        item(text).check(ViewAssertions.matches(ViewMatchers.isChecked()))

    fun assertItemChecked(@StringRes textRes: Int) = assertItemChecked(ui.getString(textRes))

    fun assertItemNotChecked(text: String) =
        item(text).check(ViewAssertions.matches(ViewMatchers.isNotChecked()))

    fun assertItemNotChecked(@StringRes textRes: Int) = assertItemNotChecked(ui.getString(textRes))

    private fun item(text: String) = onView(
        Matchers.allOf(
            ViewMatchers.withText(text),
            ViewMatchers.isDescendantOfA(ViewMatchers.withId(androidx.appcompat.R.id.select_dialog_listview)),
        )
    ).inRoot(RootMatchers.isDialog())

    /**
     * A probe rather than an assertion: callers dismiss dialogs that may already be gone, and Espresso's
     * own matching reports absence by throwing.
     */
    private fun positiveButtonShown(): Boolean {
        var shown = false
        try {
            onView(ViewMatchers.withId(android.R.id.button1)).check { view, _ -> shown = view?.isShown == true }
        } catch (_: NoActivityResumedException) {
            // ignore
        }
        return shown
    }

    private fun confirmIfVisible() {
        if (positiveButtonShown()) {
            BaristaDialogInteractions.clickDialogPositiveButton()
        }
    }

    private companion object {
        const val DIALOG_TIMEOUT = 1_000L

        /** Longer: this dialog is built on a coroutine Espresso's sync does not track, unlike [DIALOG_TIMEOUT]. */
        const val DIALOG_FROM_NOTIFICATION_TIMEOUT = 5_000L
    }
}
