package com.futsch1.medtimer.medicine.editors

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimeHelper.TimePickerWrapper
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.TimeFormat

private const val DEFAULT_TIME = 480

class TimeEditor(
    private val fragmentActivity: FragmentActivity,
    private val timeEdit: TextInputEditText,
    initialTime: Int,
    val timeChangedCallback: (minutes: Int) -> Unit,
    private val durationHintText: Int?
) {
    init {
        timeEdit.setText(
            if (durationHintText == null)
                TimeHelper.minutesToTimeString(timeEdit.context, initialTime.toLong()) else
                TimeHelper.minutesToDurationString(initialTime.toLong())
        )

        timeEdit.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            when {
                hasFocus && durationHintText != null ->
                    editDuration()

                hasFocus -> editTime()
            }
        }
    }

    private fun editDuration() {
        var startMinutes = TimeHelper.durationStringToMinutes(timeEdit.getText().toString())
        if (startMinutes < 0) {
            startMinutes = DEFAULT_TIME
        }
        TimePickerWrapper(fragmentActivity, durationHintText!!, TimeFormat.CLOCK_24H)
            .show(startMinutes / 60, startMinutes % 60) { minutes: Int ->
                val selectedTime = TimeHelper.minutesToDurationString(minutes.toLong())
                timeEdit.setText(selectedTime)
                timeChangedCallback(minutes)
            }
    }

    private fun editTime() {
        var startMinutes =
            TimeHelper.timeStringToMinutes(timeEdit.context, timeEdit.getText().toString())
        if (startMinutes < 0) {
            startMinutes = Reminder.DEFAULT_TIME
        }
        TimePickerWrapper(fragmentActivity).show(
            startMinutes / 60, startMinutes % 60
        ) { minutes: Int ->
            val selectedTime =
                TimeHelper.minutesToTimeString(timeEdit.context, minutes.toLong())
            timeEdit.setText(selectedTime)
            timeChangedCallback(minutes)
        }
    }

    fun getMinutes(): Int {
        if (durationHintText != null) {
            return TimeHelper.durationStringToMinutes(timeEdit.getText().toString())
        }
        return TimeHelper.timeStringToMinutes(timeEdit.context, timeEdit.getText().toString())
    }

}