package com.futsch1.medtimer.core.common.helpers

import android.text.format.DateFormat
import androidx.annotation.StringRes
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimePickerDialogFactory @Inject constructor(private val localeContextAccessor: LocaleContextAccessor) {
    companion object {
        const val DIALOG_TAG = "time_picker"
    }

    private val autoTimeFormat: Int
        get() =
            if (DateFormat.is24HourFormat(localeContextAccessor.getLocaleAwareContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

    /**
     * A null [timeFormat] follows the system 12- or 24-hour setting. [title] takes precedence over [titleText]. With
     * [negativeButtonText] set, the Cancel button becomes that action and hands the picked minutes to [onNegativeButtonClick];
     * back and outside tap still cancel without a callback.
     */
    fun create(
        hourOfDay: Int,
        minute: Int,
        @StringRes titleText: Int? = null,
        timeFormat: Int? = null,
        title: CharSequence? = null,
        @StringRes negativeButtonText: Int? = null,
        onNegativeButtonClick: ((minutes: Int) -> Unit)? = null,
        onPositiveButtonClick: (minutes: Int) -> Unit
    ): MaterialTimePicker {
        val timePickerDialog = MaterialTimePicker.Builder()
            .setTimeFormat(timeFormat ?: autoTimeFormat)
            .setHour(hourOfDay)
            .setMinute(minute)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .apply {
                if (title != null) {
                    setTitleText(title)
                } else if (titleText != null) {
                    setTitleText(titleText)
                }
                if (negativeButtonText != null) setNegativeButtonText(negativeButtonText)
            }.build()

        timePickerDialog.addOnPositiveButtonClickListener { onPositiveButtonClick(timePickerDialog.hour * 60 + timePickerDialog.minute) }
        if (onNegativeButtonClick != null) {
            timePickerDialog.addOnNegativeButtonClickListener { onNegativeButtonClick(timePickerDialog.hour * 60 + timePickerDialog.minute) }
        }

        return timePickerDialog
    }

    fun create(
        localTime: LocalTime,
        @StringRes titleText: Int? = null,
        timeFormat: Int? = null,
        title: CharSequence? = null,
        @StringRes negativeButtonText: Int? = null,
        onNegativeButtonClick: ((minutes: Int) -> Unit)? = null,
        onPositiveButtonClick: (minutes: Int) -> Unit
    ): MaterialTimePicker {
        return create(localTime.hour, localTime.minute, titleText, timeFormat, title, negativeButtonText, onNegativeButtonClick, onPositiveButtonClick)
    }
}
