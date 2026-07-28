package com.futsch1.medtimer.robots

import android.text.format.DateFormat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
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

    fun pickDate(date: Date) {
        clickOn(com.google.android.material.R.id.mtrl_picker_header_toggle)
        writeTo(com.google.android.material.R.id.mtrl_picker_text_input_date, inputFormat.format(date))
        closeKeyboard()
        clickOn(com.google.android.material.R.id.confirm_button)
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

        clickOn(com.google.android.material.R.id.material_timepicker_mode_button)
        writeTo(com.google.android.material.R.id.material_hour_text_input, hour.toString())
        // Close the keyboard before clicking the minute field – on tablets the soft keyboard
        // causes the time-picker dialog to be shifted (adjustPan) and the minute field leaves
        // the global visible rect, making Espresso unable to click it and eventually
        // dismissing the dialog entirely.
        closeKeyboard()
        clickOn(com.google.android.material.R.id.material_minute_text_input)
        onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(com.google.android.material.R.id.material_minute_text_input)),
                ViewMatchers.isAssignableFrom(android.widget.EditText::class.java)
            )
        ).perform(ViewActions.replaceText(minute.toString()))
        closeKeyboard()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)
    }

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
}
