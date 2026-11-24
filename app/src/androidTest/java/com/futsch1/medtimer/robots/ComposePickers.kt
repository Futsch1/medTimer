package com.futsch1.medtimer.robots

import android.text.format.DateFormat
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.component.PickerTestTags
import java.time.LocalDate
import java.time.LocalTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale
import androidx.compose.material3.R as M3R

/**
 * The Compose time and date picker dialogs; [MaterialPickers] drives the View-based ones.
 * Values are typed in keyboard mode so neither the dial layout nor the locale's clock face matters.
 */
class ComposePickers(private val ui: ComposeUi) {

    private val timePicker get() = ui.scope(PickerTestTags.TIME_PICKER)
    private val datePicker get() = ui.scope(PickerTestTags.DATE_PICKER)

    private val hourField get() = hasSetTextAction() and hasContentDescription(ui.getString(M3R.string.m3c_time_picker_hour_text_field), substring = true)
    private val minuteField get() = hasSetTextAction() and hasContentDescription(ui.getString(M3R.string.m3c_time_picker_minute_text_field), substring = true)

    /** Accepts the time already shown, for the dialogs that open pre-filled with now. */
    fun confirmTime() {
        timePicker.awaitSelfExists()
        confirm()
    }

    fun pickTime(time: LocalTime) {
        timePicker.awaitSelfExists()
        timePicker.click(hasContentDescription(ui.getString(M3R.string.m3c_time_picker_toggle_keyboard)))
        timePicker.awaitExists(hourField)

        val is24Hour = DateFormat.is24HourFormat(InstrumentationRegistry.getInstrumentation().targetContext)
        val hour = if (is24Hour) time.hour else (time.hour % 12).takeIf { it != 0 } ?: 12
        timePicker.node(hourField).performTextReplacement(hour.toString())
        timePicker.node(minuteField).performTextReplacement(time.minute.toString())
        if (!is24Hour) {
            val period = if (time.hour < 12) M3R.string.m3c_time_picker_am else M3R.string.m3c_time_picker_pm
            timePicker.click(hasText(ui.getString(period)) and hasClickAction())
        }
        confirm()
    }

    fun pickDate(date: LocalDate) {
        datePicker.awaitSelfExists()
        datePicker.click(hasContentDescription(ui.getString(M3R.string.m3c_date_picker_switch_to_input_mode)))
        datePicker.awaitExists(hasSetTextAction())
        datePicker.node(hasSetTextAction()).performTextReplacement(dateInputDigits(date))
        confirm(datePicker)
    }

    private fun confirm(picker: UiScope = timePicker) {
        picker.click(hasText(ui.getString(R.string.ok)) and hasClickAction())
        picker.awaitGone()
    }

    /** The date field takes digits only, in the order of the locale's short date pattern, as Material normalizes it. */
    private fun dateInputDigits(date: LocalDate): String {
        val locale = Locale.getDefault()
        val digitsPattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale)
            .replace("[^dMy]".toRegex(), "")
            .replace("d{1,2}".toRegex(), "dd")
            .replace("M{1,2}".toRegex(), "MM")
            .replace("y{1,4}".toRegex(), "yyyy")
        return DateTimeFormatter.ofPattern(digitsPattern, locale).format(date)
    }
}
