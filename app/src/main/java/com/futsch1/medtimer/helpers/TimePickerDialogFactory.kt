package com.futsch1.medtimer.helpers

import android.text.format.DateFormat
import androidx.annotation.StringRes
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimePickerDialogFactory @Inject constructor(private val localeContextAccessor: LocaleContextAccessor) {
    companion object {
        const val DIALOG_TAG = "time_picker"
    }

    private val autoTimeFormat: Int get() =
        if (DateFormat.is24HourFormat(localeContextAccessor.getLocaleAwareContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

    fun create(
        hourOfDay: Int,
        minute: Int,
        @StringRes titleText: Int? = null,
        timeFormat: Int = autoTimeFormat,
        onPositiveButtonClick: (minutes: Int) -> Unit
    ): MaterialTimePicker {
        val timePickerDialog = MaterialTimePicker.Builder()
            .setTimeFormat(timeFormat)
            .setHour(hourOfDay)
            .setMinute(minute)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .apply {
                if (titleText != null) setTitleText(titleText)
            }.build()

        timePickerDialog.addOnPositiveButtonClickListener { onPositiveButtonClick(timePickerDialog.hour * 60 + timePickerDialog.minute) }

        return timePickerDialog
    }
}
