package com.futsch1.medtimer.robots

import android.text.format.DateFormat
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.futsch1.medtimer.utilities.awaitView
import com.futsch1.medtimer.utilities.viewAppears
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale
import kotlin.test.fail

/**
 * The Material time and date picker dialogs. Locale (12- vs 24-hour, the date input pattern) and
 * the dialogs' own layout quirks are implementation - callers pass the value they want picked.
 */
class MaterialPickers {

    fun pickTime(time: LocalTime) = enterTime(time.hour, time.minute, isDuration = false)

    fun pickDuration(hours: Int, minutes: Int) = enterTime(hours, minutes, isDuration = true)

    /** Accepts the time already shown, for the dialogs that open pre-filled with now. */
    fun confirmTime() = confirmAndAwaitDismissal(com.google.android.material.R.id.material_timepicker_ok_button)

    fun pickDate(date: Date) {
        awaitPicker(com.google.android.material.R.id.confirm_button)
        awaitView(ViewMatchers.withId(com.google.android.material.R.id.mtrl_picker_header_toggle))
        switchToTextInput(com.google.android.material.R.id.mtrl_picker_header_toggle, dateTextInput)
        writeTo(com.google.android.material.R.id.mtrl_picker_text_input_date, inputFormat.format(date))
        closeKeyboard()
        confirmAndAwaitDismissal(com.google.android.material.R.id.confirm_button)
    }

    /** Until the picker's window is up, Espresso resolves the screen behind it as the root. */
    private fun awaitPicker(confirmButtonId: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        UiDevice.getInstance(instrumentation)
            .wait(Until.hasObject(selector(confirmButtonId)), DISMISSAL_TIMEOUT)
    }

    /** A picker left standing swallows the next tap, so the dismissal is retried and then insisted on. */
    private fun confirmAndAwaitDismissal(buttonId: Int) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val confirmButton = selector(buttonId)

        repeat(CONFIRM_ATTEMPTS) { attempt ->
            if (attempt == 0 || viewAppears(displayedView(buttonId))) {
                clickOn(buttonId)
            }
            if (device.wait(Until.gone(confirmButton), DISMISSAL_TIMEOUT)) return
        }
        fail("The picker was still up after $CONFIRM_ATTEMPTS taps on its confirm button")
    }

    private fun selector(viewId: Int) =
        By.res(InstrumentationRegistry.getInstrumentation().targetContext.resources.getResourceName(viewId))

    private fun switchToTextInput(toggleId: Int, field: Matcher<View>) {
        repeat(MODE_TOGGLE_ATTEMPTS) {
            if (viewAppears(field)) return
            clickOn(toggleId)
            viewAppears(field, MODE_SETTLE_TIMEOUT)
        }
        awaitView(field)
    }

    private fun enterTime(hour: Int, minute: Int, isDuration: Boolean) {
        awaitPicker(com.google.android.material.R.id.material_timepicker_ok_button)

        var hour = hour
        if (!DateFormat.is24HourFormat(InstrumentationRegistry.getInstrumentation().targetContext) && !isDuration) {
            clickOn(com.google.android.material.R.id.material_clock_period_am_button)
            if (hour == 12) {
                clickOn(com.google.android.material.R.id.material_clock_period_pm_button)
            }
            if (hour > 12) {
                hour -= 12
                clickOn(com.google.android.material.R.id.material_clock_period_pm_button)
            }
            if (hour == 0) {
                hour = 12
            }
        }

        switchToTextInput(com.google.android.material.R.id.material_timepicker_mode_button, hourTextInput)
        writeTo(com.google.android.material.R.id.material_hour_text_input, hour.toString())
        closeKeyboard()
        clickOn(com.google.android.material.R.id.material_minute_text_input)
        onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(com.google.android.material.R.id.material_minute_text_input)),
                ViewMatchers.isAssignableFrom(android.widget.EditText::class.java)
            )
        ).perform(ViewActions.replaceText(minute.toString()))
        closeKeyboard()
        confirmAndAwaitDismissal(com.google.android.material.R.id.material_timepicker_ok_button)
    }

    private val hourTextInput = displayedView(com.google.android.material.R.id.material_hour_text_input)

    private val dateTextInput = displayedView(com.google.android.material.R.id.mtrl_picker_text_input_date)

    private fun displayedView(viewId: Int): Matcher<View> =
        Matchers.allOf(ViewMatchers.withId(viewId), ViewMatchers.isDisplayed())

    private val inputFormat: SimpleDateFormat
        // Taken from UtcDates in Material DatePicker
        get() {
            var pattern =
                (java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.getDefault()) as SimpleDateFormat)
                    .toPattern()
            pattern = asInputFormat(pattern)
            val format = SimpleDateFormat(pattern, Locale.getDefault())
            format.isLenient = false
            return format
        }

    private fun asInputFormat(localeFormat: String): String {
        return localeFormat
            .replace("[^dMy/\\-.]".toRegex(), "")
            .replace("d{1,2}".toRegex(), "dd")
            .replace("M{1,2}".toRegex(), "MM")
            .replace("y{1,4}".toRegex(), "yyyy")
            .replace("\\.$".toRegex(), "") // Removes a dot suffix that appears in some formats
            .replace("My".toRegex(), "M/y") // Edge case for the Kako locale
    }

    private companion object {
        const val MODE_TOGGLE_ATTEMPTS = 3
        const val MODE_SETTLE_TIMEOUT = 2_000L
        const val DISMISSAL_TIMEOUT = 5_000L
        const val CONFIRM_ATTEMPTS = 3
    }
}
