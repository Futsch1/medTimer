package com.futsch1.medtimer.core.common.helpers

import android.text.format.DateUtils
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
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

    /**
     * With [maxDate] set, later days are disabled and the calendar ends on the month of [maxDate].
     * [onCancel] runs on the Cancel button and on back or outside tap, never after OK.
     */
    fun create(
        startDate: LocalDate?,
        maxDate: LocalDate? = null,
        onCancel: (() -> Unit)? = null,
        onPositiveButtonClick: (daysSinceEpoch: Long) -> Unit
    ): MaterialDatePicker<Long> {
        val effectiveStartDate = startDate ?: LocalDate.now()
        val datePickerDialog = MaterialDatePicker.Builder.datePicker()
            .setSelection(effectiveStartDate.toEpochDay() * DateUtils.DAY_IN_MILLIS)
            .apply {
                if (maxDate != null) setCalendarConstraints(maxDateConstraints(maxDate))
            }
            .build()
        datePickerDialog.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { selectedDate ->
            onPositiveButtonClick(selectedDate / DateUtils.DAY_IN_MILLIS)
        })
        if (onCancel != null) {
            // Not addOnDismissListener: dismiss also fires after OK.
            datePickerDialog.addOnNegativeButtonClickListener { onCancel() }
            datePickerDialog.addOnCancelListener { onCancel() }
        }
        return datePickerDialog
    }

    // The picker works on UTC midnights, so the local max date maps to its UTC midnight; that keeps the
    // allowed range on the local calendar day rather than the UTC day of the current instant.
    private fun maxDateConstraints(maxDate: LocalDate): CalendarConstraints {
        val maxDateMillis = maxDate.toEpochDay() * DateUtils.DAY_IN_MILLIS
        return CalendarConstraints.Builder()
            .setEnd(maxDateMillis)
            .setValidator(DateValidatorPointBackward.before(maxDateMillis))
            .build()
    }
}
