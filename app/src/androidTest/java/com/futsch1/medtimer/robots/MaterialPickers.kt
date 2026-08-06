package com.futsch1.medtimer.robots

import android.text.format.DateFormat
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.futsch1.medtimer.utilities.awaitView
import com.futsch1.medtimer.utilities.viewAppears
import com.futsch1.medtimer.utilities.viewDisappears
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale

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
        awaitView(ViewMatchers.withId(com.google.android.material.R.id.mtrl_picker_header_toggle))
        switchToTextInput(com.google.android.material.R.id.mtrl_picker_header_toggle, dateTextInput)
        writeTo(com.google.android.material.R.id.mtrl_picker_text_input_date, inputFormat.format(date))
        closeKeyboard()
        confirmAndAwaitDismissal(com.google.android.material.R.id.confirm_button)
    }

    /** The picker goes down with a fragment transaction Espresso does not idle on, so the next tap can hit it. */
    private fun confirmAndAwaitDismissal(buttonId: Int) {
        clickOn(buttonId)
        viewDisappears(ViewMatchers.withId(buttonId))
    }

    private fun switchToTextInput(toggleId: Int, field: Matcher<View>) {
        repeat(MODE_TOGGLE_ATTEMPTS) {
            if (viewAppears(field)) return
            clickOn(toggleId)
            viewAppears(field, MODE_SETTLE_TIMEOUT)
        }
        awaitView(field)
    }

    private fun enterTime(hour: Int, minute: Int, isDuration: Boolean) {
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
    }
}
