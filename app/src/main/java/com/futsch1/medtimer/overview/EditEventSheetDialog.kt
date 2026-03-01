package com.futsch1.medtimer.overview

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimeHelper.TimePickerWrapper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.sidesheet.SideSheetDialog
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class EditEventSheetDialog(val activity: FragmentActivity, val reminderEvent: ReminderEvent, val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val editEventTakenDate: EditText
    private val editEventTakenTimestamp: EditText
    private val editEventRemindedDate: EditText
    private val editEventRemindedTimestamp: EditText
    private val editEventAmount: EditText
    private val editEventName: EditText
    private val editEventNotes: EditText
    private val editEventToggleGroup: MaterialButtonToggleGroup

    init {
        val editEventSheetDialog =
            if (activity.resources.configuration.orientation == ORIENTATION_PORTRAIT) BottomSheetDialog(activity) else SideSheetDialog(activity)
        editEventSheetDialog.setContentView(R.layout.sheet_edit_event)

        editEventName = editEventSheetDialog.findViewById<EditText>(R.id.editEventName)!!
        editEventAmount = editEventSheetDialog.findViewById<EditText>(R.id.editEventAmount)!!
        editEventRemindedTimestamp = editEventSheetDialog.findViewById<EditText>(R.id.editEventRemindedTimestamp)!!
        editEventRemindedDate = editEventSheetDialog.findViewById<EditText>(R.id.editEventRemindedDate)!!
        editEventTakenTimestamp = editEventSheetDialog.findViewById<EditText>(R.id.editEventTakenTimestamp)!!
        editEventTakenDate = editEventSheetDialog.findViewById<EditText>(R.id.editEventTakenDate)!!
        editEventNotes = editEventSheetDialog.findViewById<EditText>(R.id.editEventNotes)!!
        editEventToggleGroup = editEventSheetDialog.findViewById<MaterialButtonToggleGroup>(R.id.editEventToggleGroup)!!

        setupData(editEventSheetDialog)

        editEventSheetDialog.setOnDismissListener {
            saveData()
        }
        editEventSheetDialog.findViewById<MaterialToolbar>(R.id.editEventSideSheetToolbar)?.setNavigationOnClickListener {
            saveData()
            editEventSheetDialog.dismiss()
        }
        editEventSheetDialog.show()
    }

    private fun saveData() {
        reminderEvent.medicineName = editEventName.getText().toString()
        reminderEvent.amount = editEventAmount.getText().toString()

        reminderEvent.remindedTimestamp = processDateTimeEdits(reminderEvent.remindedTimestamp, editEventRemindedTimestamp, editEventRemindedDate)
        reminderEvent.processedTimestamp = processDateTimeEdits(reminderEvent.processedTimestamp, editEventTakenTimestamp, editEventTakenDate)

        reminderEvent.notes = editEventNotes.getText().toString()
        if (editEventToggleGroup.isVisible) {
            reminderEvent.status = if (editEventToggleGroup.checkedButtonId == R.id.takenToggleButton) {
                ReminderEvent.ReminderStatus.TAKEN
            } else {
                ReminderEvent.ReminderStatus.SKIPPED
            }
        }

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

    private fun setupData(editEventView: AppCompatDialog) {
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
        setupToggleGroup(reminderEvent)
    }

    private fun setupToggleGroup(entity: ReminderEvent) {
        when (entity.status) {
            ReminderEvent.ReminderStatus.TAKEN -> {
                editEventToggleGroup.check(R.id.takenToggleButton)
            }

            ReminderEvent.ReminderStatus.SKIPPED, ReminderEvent.ReminderStatus.RAISED -> {
                editEventToggleGroup.check(R.id.skippedToggleButton)
            }

            else -> {
                editEventToggleGroup.visibility = View.GONE
            }
        }
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

    private fun configureTakenText(dialog: AppCompatDialog, entity: ReminderEvent) {
        val takenText = dialog.findViewById<TextView>(R.id.takenText)!!
        when (entity.status) {
            ReminderEvent.ReminderStatus.TAKEN -> {
                takenText.setText(R.string.taken)
            }

            ReminderEvent.ReminderStatus.SKIPPED -> {
                takenText.setText(R.string.skipped)
            }

            ReminderEvent.ReminderStatus.ACKNOWLEDGED -> {
                takenText.setText(R.string.acknowledged)
                dialog.findViewById<TextInputLayout>(R.id.editEventAmountLayout)?.hint = ""
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