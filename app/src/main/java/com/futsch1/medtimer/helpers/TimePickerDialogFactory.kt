package com.futsch1.medtimer.helpers

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.StringRes
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimePickerDialogFactory @Inject constructor(@ApplicationContext context: Context) {
    companion object {
        const val DIALOG_TAG = "time_picker"
    }

    private val localeAwareContext = LocaleContextWrapper(context)

    private val autoTimeFormat: Int get() =
        if (DateFormat.is24HourFormat(localeAwareContext)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

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
