package com.futsch1.medtimer.core.common.helpers

import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
        val utcStartMillis = effectiveStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerDialog = MaterialDatePicker.Builder.datePicker()
            .setSelection(utcStartMillis)
            .build()
        datePickerDialog.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { selectedDate ->
            val selectedEpochDay = Instant.ofEpochMilli(selectedDate)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .toEpochDay()
            onPositiveButtonClick(selectedEpochDay)
        })
        return datePickerDialog
    }
}
