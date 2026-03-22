package com.futsch1.medtimer.helpers

import android.text.format.DateUtils
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatePickerDialogFactory @Inject constructor() {
    companion object {
        const val DIALOG_TAG = "date_picker"
    }

    fun create(
        startDate: LocalDate?,
        onPositiveButtonClick: (daysSinceEpoch: Long) -> Unit
    ): MaterialDatePicker<Long> {
        val effectiveStartDate = startDate ?: LocalDate.now()
        val datePickerDialog = MaterialDatePicker.Builder.datePicker()
            .setSelection(effectiveStartDate.toEpochDay() * DateUtils.DAY_IN_MILLIS)
            .build()
        datePickerDialog.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { selectedDate ->
            onPositiveButtonClick(selectedDate / DateUtils.DAY_IN_MILLIS)
        })
        return datePickerDialog
    }
}
