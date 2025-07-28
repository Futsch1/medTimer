package com.futsch1.medtimer.medicine.editors

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimeHelper.TimePickerWrapper
import com.google.android.material.textfield.TextInputEditText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class DateTimeEditor(
    private val fragmentActivity: FragmentActivity,
    private val dateTimeEdit: TextInputEditText,
    initialDateTimeSecondsSinceEpoch: Long,
) {
    init {
        dateTimeEdit.setText(
            TimeHelper.toLocalizedDatetimeString(
                fragmentActivity.baseContext,
                initialDateTimeSecondsSinceEpoch
            )
        )
        dateTimeEdit.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            onFocusChangeListener(hasFocus)
        }
    }

    private fun onFocusChangeListener(hasFocus: Boolean) {
        if (hasFocus) {
            val startInstant = Instant.ofEpochSecond(getDateTimeSecondsSinceEpoch())
            val dateTime = startInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()

            TimeHelper.DatePickerWrapper(fragmentActivity)
                .show(
                    dateTime.toLocalDate()
                ) { selectedDate ->
                    TimePickerWrapper(fragmentActivity).show(
                        dateTime.hour, dateTime.minute
                    ) { selectedTime ->
                        val selectedLocalDateTime = LocalDateTime.of(
                            LocalDate.ofEpochDay(selectedDate),
                            LocalTime.of(selectedTime / 60, selectedTime % 60)
                        )
                        dateTimeEdit.setText(
                            TimeHelper.toLocalizedDatetimeString(
                                fragmentActivity.baseContext,
                                selectedLocalDateTime.toEpochSecond(
                                    ZoneId.systemDefault().rules.getOffset(
                                        selectedLocalDateTime
                                    )
                                )
                            )
                        )
                    }
                }
        }
    }

    fun getDateTimeSecondsSinceEpoch(): Long {
        return TimeHelper.dateTimeStringToSecondsSinceEpoch(
            fragmentActivity.baseContext,
            dateTimeEdit.getText().toString()
        )
    }
}