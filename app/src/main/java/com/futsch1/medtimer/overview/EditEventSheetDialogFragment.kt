package com.futsch1.medtimer.overview

import android.content.DialogInterface
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.helpers.DatePickerDialogFactory
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.helpers.TimePickerDialogFactory
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.sidesheet.SideSheetDialog
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.time.LocalDate

@AndroidEntryPoint
class EditEventSheetDialogFragment : DialogFragment() {
    private lateinit var editEventTakenDate: EditText
    private lateinit var editEventTakenTimestamp: EditText
    private lateinit var editEventRemindedDate: EditText
    private lateinit var editEventRemindedTimestamp: EditText
    private lateinit var editEventAmount: EditText
    private lateinit var editEventName: EditText
    private lateinit var editEventNotes: EditText
    private lateinit var editEventToggleGroup: MaterialButtonToggleGroup

    private val viewModel: EditEventViewModel by viewModels()

    @javax.inject.Inject
    lateinit var timePickerDialogFactory: TimePickerDialogFactory

    @javax.inject.Inject
    lateinit var datePickerDialogFactory: DatePickerDialogFactory

    @javax.inject.Inject
    lateinit var timeFormatter: TimeFormatter

    companion object {
        fun newInstance(reminderEventId: Int): EditEventSheetDialogFragment {
            return EditEventSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(EditEventViewModel.ARG_REMINDER_EVENT_ID, reminderEventId)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AppCompatDialog {
        val context = requireContext()
        val editEventSheetDialog =
            if (context.resources.configuration.orientation == ORIENTATION_PORTRAIT) {
                BottomSheetDialog(context)
            } else {
                SideSheetDialog(context)
            }

        editEventSheetDialog.setContentView(R.layout.sheet_edit_event)

        editEventName = editEventSheetDialog.requireViewById(R.id.editEventName)
        editEventAmount = editEventSheetDialog.requireViewById(R.id.editEventAmount)
        editEventRemindedTimestamp = editEventSheetDialog.requireViewById(R.id.editEventRemindedTimestamp)
        editEventRemindedDate = editEventSheetDialog.requireViewById(R.id.editEventRemindedDate)
        editEventTakenTimestamp = editEventSheetDialog.requireViewById(R.id.editEventTakenTimestamp)
        editEventTakenDate = editEventSheetDialog.requireViewById(R.id.editEventTakenDate)
        editEventNotes = editEventSheetDialog.requireViewById(R.id.editEventNotes)
        editEventToggleGroup = editEventSheetDialog.requireViewById(R.id.editEventToggleGroup)

        // Text fields: flow sets initial value; doAfterTextChanged writes back.
        // Guard prevents setText from firing when the EditText already has the same text,
        // which would otherwise reset the cursor position.
        viewModel.medicineName.onEach { if (editEventName.text.toString() != it) editEventName.setText(it) }.launchIn(lifecycleScope)
        editEventName.doAfterTextChanged { viewModel.medicineName.value = it?.toString() ?: "" }

        viewModel.amount.onEach { if (editEventAmount.text.toString() != it) editEventAmount.setText(it) }.launchIn(lifecycleScope)
        editEventAmount.doAfterTextChanged { viewModel.amount.value = it?.toString() ?: "" }

        viewModel.notes.onEach { if (editEventNotes.text.toString() != it) editEventNotes.setText(it) }.launchIn(lifecycleScope)
        editEventNotes.doAfterTextChanged { viewModel.notes.value = it?.toString() ?: "" }

        // Time/date display strings: reactive to picker changes
        viewModel.remindedTimeString.onEach { if (editEventRemindedTimestamp.text.toString() != it) editEventRemindedTimestamp.setText(it) }
            .launchIn(lifecycleScope)
        setupTimePicker(editEventRemindedTimestamp, { viewModel.remindedMinutes }, { viewModel.remindedMinutes = it })

        viewModel.remindedDateString.onEach { if (editEventRemindedDate.text.toString() != it) editEventRemindedDate.setText(it) }.launchIn(lifecycleScope)
        setupDatePicker(editEventRemindedDate, { viewModel.remindedDate }, { viewModel.remindedDate = it })

        // Status-dependent UI: runs once when actual status arrives
        viewModel.reminderStatus.filterNotNull().take(1).onEach { status ->
            configureTakenText(editEventSheetDialog, status)
            if (status != ReminderEventEntity.ReminderStatus.RAISED) {
                viewModel.processedTimeString.onEach { if (editEventTakenTimestamp.text.toString() != it) editEventTakenTimestamp.setText(it) }
                    .launchIn(lifecycleScope)
                setupTimePicker(editEventTakenTimestamp, { viewModel.processedMinutes }, { viewModel.processedMinutes = it })

                viewModel.processedDateString.onEach { if (editEventTakenDate.text.toString() != it) editEventTakenDate.setText(it) }.launchIn(lifecycleScope)
                setupDatePicker(editEventTakenDate, { viewModel.processedDate }, { viewModel.processedDate = it })
            } else {
                editEventTakenTimestamp.visibility = View.GONE
                editEventTakenDate.visibility = View.GONE
            }
            setupToggleGroup(status)
        }.launchIn(lifecycleScope)

        editEventToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.status = if (checkedId == R.id.takenToggleButton)
                    ReminderEventEntity.ReminderStatus.TAKEN
                else
                    ReminderEventEntity.ReminderStatus.SKIPPED
            }
        }

        editEventSheetDialog.findViewById<MaterialToolbar>(R.id.editEventSideSheetToolbar)?.setNavigationOnClickListener {
            editEventSheetDialog.dismiss()
        }

        return editEventSheetDialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        requireActivity().lifecycleScope.launch {
            viewModel.updateEvent()
        }
        super.onDismiss(dialog)
    }

    private fun setupToggleGroup(status: ReminderEventEntity.ReminderStatus) {
        when (status) {
            ReminderEventEntity.ReminderStatus.TAKEN -> editEventToggleGroup.check(R.id.takenToggleButton)
            ReminderEventEntity.ReminderStatus.SKIPPED, ReminderEventEntity.ReminderStatus.RAISED -> editEventToggleGroup.check(R.id.skippedToggleButton)
            else -> editEventToggleGroup.visibility = View.GONE
        }
    }

    private fun setupTimePicker(editText: EditText, getMinutes: () -> Int, onTimePicked: (Int) -> Unit) {
        editText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) return@OnFocusChangeListener
            val startMinutes = getMinutes().takeIf { it >= 0 } ?: ReminderEntity.DEFAULT_TIME
            timePickerDialogFactory.create(startMinutes / 60, startMinutes % 60) { minutes ->
                try {
                    onTimePicked(minutes)
                } catch (_: IllegalStateException) {
                    // Intentionally empty
                }
            }.show(parentFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
        }
        editText.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: return@doAfterTextChanged
            val minutes = timeFormatter.timeStringToMinutes(text)
            if (minutes >= 0 && minutes != getMinutes()) onTimePicked(minutes)
        }
    }

    private fun setupDatePicker(editText: EditText, getDate: () -> LocalDate, onDatePicked: (LocalDate) -> Unit) {
        editText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) return@OnFocusChangeListener
            datePickerDialogFactory.create(getDate()) { daysSinceEpoch ->
                onDatePicked(LocalDate.ofEpochDay(daysSinceEpoch))
            }.show(parentFragmentManager, DatePickerDialogFactory.DIALOG_TAG)
        }
        editText.visibility = View.VISIBLE
        editText.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: return@doAfterTextChanged
            val date = timeFormatter.stringToLocalDate(text)
            if (date != null && date != getDate()) onDatePicked(date)
        }
    }

    private fun configureTakenText(dialog: AppCompatDialog, status: ReminderEventEntity.ReminderStatus) {
        val takenText = dialog.requireViewById<TextView>(R.id.takenText)
        when (status) {
            ReminderEventEntity.ReminderStatus.TAKEN -> takenText.setText(R.string.taken)
            ReminderEventEntity.ReminderStatus.SKIPPED -> takenText.setText(R.string.skipped)
            ReminderEventEntity.ReminderStatus.ACKNOWLEDGED -> {
                takenText.setText(R.string.acknowledged)
                dialog.findViewById<TextInputLayout>(R.id.editEventAmountLayout)?.hint = ""
            }

            else -> takenText.visibility = View.GONE
        }
    }
}
