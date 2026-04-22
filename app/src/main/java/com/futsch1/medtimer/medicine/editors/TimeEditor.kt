package com.futsch1.medtimer.medicine.editors

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.model.ReminderTime
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.TimeFormat
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

private const val DEFAULT_TIME = 480

class TimeEditor @AssistedInject constructor(
    @Assisted private val fragmentActivity: FragmentActivity,
    @Assisted private val timeEdit: TextInputEditText,
    @Assisted initialTimeMinutesOfDay: Int,
    @Assisted val timeChangedCallback: (minutes: Int) -> Unit,
    @Assisted private val durationHintText: Int?,
    private val timePickerDialogFactory: TimePickerDialogFactory,
    private val timeFormatter: TimeFormatter
) {
    @AssistedFactory
    interface Factory {
        fun create(
            fragmentActivity: FragmentActivity,
            timeEdit: TextInputEditText,
            initialTimeMinutesOfDay: Int,
            timeChangedCallback: (minutes: Int) -> Unit,
            durationHintText: Int?
        ): TimeEditor
    }

    init {
        timeEdit.setText(
            if (durationHintText == null) timeFormatter.minutesToTimeString(initialTimeMinutesOfDay) else TimeHelper.minutesToDurationString(
                initialTimeMinutesOfDay
            )
        )

        timeEdit.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            when {
                hasFocus && durationHintText != null ->
                    editDuration()

                hasFocus -> editTime()
            }
        }
        timeEdit.setOnClickListener {
            timeEdit.onFocusChangeListener.onFocusChange(timeEdit, true)
        }
    }

    private fun editDuration() {
        var startMinutes = TimeHelper.durationStringToMinutes(timeEdit.getText().toString())
        if (startMinutes < 0) {
            startMinutes = DEFAULT_TIME
        }
        timePickerDialogFactory
            .create(startMinutes / 60, startMinutes % 60, durationHintText!!, TimeFormat.CLOCK_24H) { minutes: Int ->
                val selectedTime = TimeHelper.minutesToDurationString(minutes)
                timeEdit.setText(selectedTime)
                timeChangedCallback(minutes)
            }.show(fragmentActivity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
    }

    private fun editTime() {
        var startMinutes =
            timeFormatter.timeStringToMinutes(timeEdit.getText().toString())
        if (startMinutes < 0) {
            startMinutes = ReminderTime.DEFAULT_TIME
        }
        timePickerDialogFactory.create(
            startMinutes / 60, startMinutes % 60
        ) { minutes: Int ->
            val selectedTime =
                timeFormatter.minutesToTimeString(minutes)
            timeEdit.setText(selectedTime)
            timeChangedCallback(minutes)
        }.show(fragmentActivity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
    }

    fun getMinutes(): Int {
        if (durationHintText != null) {
            return TimeHelper.durationStringToMinutes(timeEdit.getText().toString())
        }
        return timeFormatter.timeStringToMinutes(timeEdit.getText().toString())
    }

}