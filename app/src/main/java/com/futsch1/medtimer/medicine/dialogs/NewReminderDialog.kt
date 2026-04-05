package com.futsch1.medtimer.medicine.dialogs

import android.app.Dialog
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.helpers.AmountTextWatcher
import com.futsch1.medtimer.helpers.Interval
import com.futsch1.medtimer.medicine.editors.DateTimeEditor
import com.futsch1.medtimer.medicine.editors.IntervalEditor
import com.futsch1.medtimer.medicine.editors.TimeEditor
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderType
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime


class NewReminderDialog @AssistedInject constructor(
    @Assisted private val activity: FragmentActivity,
    @Assisted private val fullMedicine: FullMedicineEntity,
    @Assisted private val reminder: Reminder,
    private val reminderRepository: ReminderRepository,
    private val timeEditorFactory: TimeEditor.Factory,
    private val dateTimeEditorFactory: DateTimeEditor.Factory,
    private val inputMethodManager: InputMethodManager
) {
    @AssistedFactory
    interface Factory {
        fun create(activity: FragmentActivity, fullMedicine: FullMedicineEntity, reminder: Reminder): NewReminderDialog
    }

    private val dialog: Dialog = Dialog(activity)

    init {
        dialog.setContentView(R.layout.dialog_new_reminder)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupCreateReminder()
        setupVisibilities()
        dialog.findViewById<MaterialButton>(R.id.cancelCreateReminder).setOnClickListener {
            dialog.dismiss()
        }

        startEditAmount()
        dialog.show()
    }

    private fun startEditAmount() {
        val textInputEditText = dialog.findViewById<TextInputEditText>(R.id.editAmount)
        textInputEditText.requestFocus()
        textInputEditText.postDelayed({
            inputMethodManager.showSoftInput(textInputEditText, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
        if (fullMedicine.isStockManagementActive) {
            textInputEditText.addTextChangedListener(
                AmountTextWatcher(
                    textInputEditText
                )
            )
        } else {
            dialog.findViewById<TextInputLayout>(R.id.editAmountLayout).isErrorEnabled = false
        }
    }

    private fun setupVisibilities() {
        val timeBasedVisibility =
            if (reminder.reminderType == ReminderType.TIME_BASED) ViewGroup.VISIBLE else ViewGroup.GONE
        val intervalBasedVisibility =
            if (reminder.isInterval) ViewGroup.VISIBLE else ViewGroup.GONE
        val continuousIntervalVisibility =
            if (reminder.reminderType == ReminderType.CONTINUOUS_INTERVAL) ViewGroup.VISIBLE else ViewGroup.GONE
        val windowedIntervalVisibility =
            if (reminder.reminderType == ReminderType.WINDOWED_INTERVAL) ViewGroup.VISIBLE else ViewGroup.GONE

        dialog.findViewById<TextInputLayout>(R.id.editIntervalTimeLayout).visibility =
            intervalBasedVisibility
        dialog.findViewById<MaterialButtonToggleGroup>(R.id.intervalUnit).visibility =
            intervalBasedVisibility
        dialog.findViewById<TextInputLayout>(R.id.editIntervalStartDateTimeLayout).visibility =
            continuousIntervalVisibility
        dialog.findViewById<TextInputLayout>(R.id.editIntervalDailyStartTimeLayout).visibility =
            windowedIntervalVisibility
        dialog.findViewById<TextInputLayout>(R.id.editIntervalDailyEndTimeLayout).visibility =
            windowedIntervalVisibility
        dialog.findViewById<RadioGroup>(R.id.intervalStartType).visibility =
            intervalBasedVisibility
        dialog.findViewById<TextInputLayout>(R.id.editReminderTimeLayout).visibility =
            timeBasedVisibility
    }

    private fun setupCreateReminder(
    ) {
        val timeEditor = timeEditorFactory.create(
            activity,
            dialog.findViewById(R.id.editReminderTime),
            reminder.time.toSecondOfDay() / 60,
            { _ -> },
            null
        )

        val intervalEditor = IntervalEditor(
            dialog.findViewById(R.id.editIntervalTime),
            dialog.findViewById(R.id.editIntervalTimeLayout),
            dialog.findViewById(R.id.intervalUnit), 12 * 60,
            if (reminder.reminderType == ReminderType.WINDOWED_INTERVAL) 24 * 60 else Interval.MAX_INTERVAL_MINUTES
        )

        val intervalStartDateTimeEditor = dateTimeEditorFactory.create(
            activity,
            dialog.findViewById(R.id.editIntervalStartDateTime),
            Instant.now().epochSecond
        )

        val dailyStartTimeEditor = timeEditorFactory.create(
            activity,
            dialog.findViewById(R.id.editIntervalDailyStartTime),
            reminder.intervalStartTimeOfDay.toSecondOfDay() / 60,
            { _ -> },
            null
        )
        val dailyEndTimeEditor = timeEditorFactory.create(
            activity,
            dialog.findViewById(R.id.editIntervalDailyEndTime),
            reminder.intervalEndTimeOfDay.toSecondOfDay() / 60,
            { _ -> },
            null
        )


        setupOnClickCreateReminder(
            timeEditor,
            intervalEditor,
            intervalStartDateTimeEditor,
            dailyStartTimeEditor,
            dailyEndTimeEditor
        )
    }

    private fun setupOnClickCreateReminder(
        timeEditor: TimeEditor,
        intervalEditor: IntervalEditor,
        intervalStartDateTimeEditor: DateTimeEditor,
        dailyStartTimeEditor: TimeEditor,
        dailyEndTimeEditor: TimeEditor
    ) {
        dialog.findViewById<MaterialButton>(R.id.createReminder).setOnClickListener {
            activity.lifecycleScope.launch {
                var updatedReminder = reminder.copy(
                    amount = dialog.findViewById<TextInputEditText>(R.id.editAmount).text.toString().trim()
                )

                val minutes = if (reminder.reminderType == ReminderType.TIME_BASED) {
                    timeEditor.getMinutes()
                } else {
                    intervalEditor.getMinutes()
                }
                updatedReminder = updatedReminder.copy(time = LocalTime.ofSecondOfDay(minutes * 60L))
                if (reminder.reminderType == ReminderType.CONTINUOUS_INTERVAL) {
                    updatedReminder = updatedReminder.copy(
                        intervalStart = Instant.ofEpochSecond(intervalStartDateTimeEditor.getDateTimeSecondsSinceEpoch()),
                        intervalStartsFromProcessed = dialog.findViewById<MaterialRadioButton>(R.id.intervalStarsFromProcessed).isChecked
                    )
                }
                if (reminder.reminderType == ReminderType.WINDOWED_INTERVAL) {
                    updatedReminder = updatedReminder.copy(
                        intervalStartTimeOfDay = LocalTime.ofSecondOfDay(dailyStartTimeEditor.getMinutes() * 60L),
                        intervalEndTimeOfDay = LocalTime.ofSecondOfDay(dailyEndTimeEditor.getMinutes() * 60L)
                    )
                }
                if (minutes >= 0 && (reminder.reminderType == ReminderType.TIME_BASED || reminder.intervalStart != Instant.EPOCH)) {
                    reminderRepository.create(updatedReminder)
                    Toast.makeText(
                        activity,
                        R.string.successfully_created_reminder,
                        Toast.LENGTH_LONG
                    ).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(activity, R.string.invalid_input, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
