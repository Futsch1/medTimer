package com.futsch1.medtimer.medicine.editors

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.helpers.DatePickerDialogFactory
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimePickerDialogFactory
import com.google.android.material.textfield.TextInputEditText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class DateTimeEditor @AssistedInject constructor(
    @Assisted private val fragmentActivity: FragmentActivity,
    @Assisted private val dateTimeEdit: TextInputEditText,
    @Assisted initialDateTimeSecondsSinceEpoch: Long,
    private val timePickerDialogFactory: TimePickerDialogFactory,
    private val datePickerDialogFactory: DatePickerDialogFactory
) {
    @AssistedFactory
    interface Factory {
        fun create(
            fragmentActivity: FragmentActivity,
            dateTimeEdit: TextInputEditText,
            initialDateTimeSecondsSinceEpoch: Long
        ): DateTimeEditor
    }

    init {
        dateTimeEdit.setText(
            TimeHelper.secondsSinceEpochToDateTimeString(
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

            datePickerDialogFactory
                .create(dateTime.toLocalDate()) { selectedDate ->
                    timePickerDialogFactory.create(
                        dateTime.hour, dateTime.minute
                    ) { selectedTime ->
                        val selectedLocalDateTime = LocalDateTime.of(
                            LocalDate.ofEpochDay(selectedDate),
                            LocalTime.of(selectedTime / 60, selectedTime % 60)
                        )
                        dateTimeEdit.setText(
                            TimeHelper.secondsSinceEpochToDateTimeString(
                                fragmentActivity.baseContext,
                                selectedLocalDateTime.toEpochSecond(
                                    ZoneId.systemDefault().rules.getOffset(
                                        selectedLocalDateTime
                                    )
                                )
                            )
                        )
                    }.show(fragmentActivity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
                }.show(fragmentActivity.supportFragmentManager, DatePickerDialogFactory.DIALOG_TAG)
        }
    }

    fun getDateTimeSecondsSinceEpoch(): Long {
        return TimeHelper.stringToSecondsSinceEpoch(
            fragmentActivity.baseContext,
            dateTimeEdit.getText().toString()
        )
    }
}