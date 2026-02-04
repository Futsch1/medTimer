package com.futsch1.medtimer.overview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimeHelper.TimePickerWrapper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.sidesheet.SideSheetDialog
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@SuppressLint("InflateParams")
class EditEventSideSheetDialog(val activity: FragmentActivity, val reminderEvent: ReminderEvent, val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val editEventTakenDate: EditText
    private val editEventTakenTimestamp: EditText
    private val editEventRemindedDate: EditText
    private val editEventRemindedTimestamp: EditText
    private val editEventAmount: EditText
    private val editEventName: EditText
    private val editEventNotes: EditText

    init {
        val editEventSideSheet = SideSheetDialog(activity)
        val editEventView: View = LayoutInflater.from(activity).inflate(R.layout.sidesheet_edit_event, null)
        editEventName = editEventView.findViewById(R.id.editEventName)
        editEventAmount = editEventView.findViewById(R.id.editEventAmount)
        editEventRemindedTimestamp = editEventView.findViewById(R.id.editEventRemindedTimestamp)
        editEventRemindedDate = editEventView.findViewById(R.id.editEventRemindedDate)
        editEventTakenTimestamp = editEventView.findViewById(R.id.editEventTakenTimestamp)
        editEventTakenDate = editEventView.findViewById(R.id.editEventTakenDate)
        editEventNotes = editEventView.findViewById(R.id.editEventNotes)

        editEventSideSheet.setContentView(editEventView)
        setupData(editEventView)

        editEventSideSheet.setOnDismissListener {
            saveData()
        }
        editEventView.findViewById<MaterialToolbar>(R.id.editEventSideSheetToolbar).setNavigationOnClickListener {
            saveData()
            editEventSideSheet.dismiss()
        }
        editEventSideSheet.show()
    }

    private fun saveData() {
        reminderEvent.medicineName = editEventName.getText().toString()
        reminderEvent.amount = editEventAmount.getText().toString()

        reminderEvent.remindedTimestamp = processDateTimeEdits(reminderEvent.remindedTimestamp, editEventRemindedTimestamp, editEventRemindedDate)
        reminderEvent.processedTimestamp = processDateTimeEdits(reminderEvent.processedTimestamp, editEventTakenTimestamp, editEventTakenDate)

        reminderEvent.notes = editEventNotes.getText().toString()

        activity.lifecycleScope.launch {
            withContext(ioDispatcher) {
                val medicineRepository = MedicineRepository(activity.application)

                medicineRepository.updateReminderEvent(reminderEvent)
            }
        }
    }

    private fun processDateTimeEdits(timestamp: Long, editTimestamp: EditText, editDate: EditText): Long {
        var timestamp = timestamp
        val minutes = TimeHelper.timeStringToMinutes(activity, editTimestamp.getText().toString())
        if (minutes >= 0) {
            timestamp = TimeHelper.changeTimeStampMinutes(timestamp, minutes)
        }
        timestamp = TimeHelper.changeTimeStampDate(
            timestamp,
            TimeHelper.stringToLocalDate(activity, editDate.getText().toString())
        )
        return timestamp
    }

    private fun setupData(editEventView: View) {
        editEventName.setText(reminderEvent.medicineName)
        editEventAmount.setText(reminderEvent.amount)

        setupEditTime(reminderEvent.remindedTimestamp, editEventRemindedTimestamp)
        setupEditDate(reminderEvent.remindedTimestamp, editEventRemindedDate)

        configureTakenText(editEventView, reminderEvent)
        if (reminderEvent.status != ReminderEvent.ReminderStatus.RAISED) {
            setupEditTime(reminderEvent.processedTimestamp, editEventTakenTimestamp)
            setupEditDate(reminderEvent.processedTimestamp, editEventTakenDate)
        } else {
            editEventTakenTimestamp.visibility = View.GONE
            editEventTakenDate.visibility = View.GONE
        }
        editEventNotes.setText(reminderEvent.notes)
    }

    private fun setupEditTime(timestamp: Long, editText: EditText) {
        editText.setText(
            TimeHelper.secondsSinceEpochToTimeString(
                editText.context,
                timestamp
            )
        )
        editText.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean -> onFocusEditTime(hasFocus, editText) }
    }

    private fun setupEditDate(timestamp: Long, editText: EditText) {
        editText.setText(
            TimeHelper.secondSinceEpochToDateString(
                activity,
                timestamp
            )
        )
        editText.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean -> onFocusEditDate(hasFocus, editText) }
        editText.visibility = View.VISIBLE
    }

    private fun configureTakenText(fragmentView: View, entity: ReminderEvent) {
        val takenText = fragmentView.findViewById<TextView>(R.id.takenText)
        when (entity.status) {
            ReminderEvent.ReminderStatus.TAKEN -> {
                takenText.setText(R.string.taken)
            }

            ReminderEvent.ReminderStatus.SKIPPED -> {
                takenText.setText(R.string.skipped)
            }

            ReminderEvent.ReminderStatus.ACKNOWLEDGED -> {
                takenText.setText(R.string.acknowledged)
                fragmentView.findViewById<TextInputLayout>(R.id.editEventAmountLayout).hint = ""
            }

            else -> {
                takenText.visibility = View.GONE
            }
        }
    }


    private fun onFocusEditTime(hasFocus: Boolean, editText: EditText) {
        if (hasFocus) {
            var startMinutes = TimeHelper.timeStringToMinutes(
                editText.context,
                editText.getText().toString()
            )
            if (startMinutes < 0) {
                startMinutes = Reminder.DEFAULT_TIME
            }
            TimePickerWrapper(activity).show(startMinutes / 60, startMinutes % 60) { minutes: Int ->
                try {
                    val selectedTime = TimeHelper.minutesToTimeString(activity, minutes.toLong())
                    editText.setText(selectedTime)
                } catch (_: IllegalStateException) {
                    // Intentionally empty
                }
            }
        }
    }


    private fun onFocusEditDate(hasFocus: Boolean, editText: EditText) {
        if (hasFocus) {
            var startDate = TimeHelper.stringToLocalDate(activity, editText.getText().toString())
            if (startDate == null) {
                startDate = LocalDate.now()
            }

            TimeHelper.DatePickerWrapper(activity).show(startDate) { daysSinceEpoch ->
                editText.setText(
                    TimeHelper.daysSinceEpochToDateString(
                        activity,
                        daysSinceEpoch
                    )
                )
            }
        }
    }

}