package com.futsch1.medtimer.medicine.editors

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.helpers.DatePickerDialogFactory
import com.futsch1.medtimer.helpers.TimeFormatter
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
    @Assisted initialInstant: Instant,
    private val timePickerDialogFactory: TimePickerDialogFactory,
    private val datePickerDialogFactory: DatePickerDialogFactory,
    private val timeFormatter: TimeFormatter
) {
    @AssistedFactory
    interface Factory {
        fun create(
            fragmentActivity: FragmentActivity,
            dateTimeEdit: TextInputEditText,
            initialDateTimeSecondsSinceEpoch: Instant
        ): DateTimeEditor
    }

    init {
        dateTimeEdit.setText(
            timeFormatter.toDateTimeString(
                initialInstant
            )
        )
        dateTimeEdit.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            onFocusChangeListener(hasFocus)
        }
    }

    private fun onFocusChangeListener(hasFocus: Boolean) {
        if (hasFocus) {
            val startInstant = getInstant() ?: Instant.EPOCH
            val dateTime = startInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()

            datePickerDialogFactory
                .create(dateTime.toLocalDate()) { selectedDate ->
                    timePickerDialogFactory.create(
                        dateTime.toLocalTime()
                    ) { selectedTime ->
                        val selectedLocalDateTime = LocalDateTime.of(
                            LocalDate.ofEpochDay(selectedDate),
                            LocalTime.of(selectedTime / 60, selectedTime % 60)
                        )
                        dateTimeEdit.setText(
                            timeFormatter.toDateTimeString(
                                selectedLocalDateTime.toInstant(
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

    fun getInstant(): Instant? {
        return timeFormatter.stringToInstant(
            dateTimeEdit.getText().toString()
        )
    }
}