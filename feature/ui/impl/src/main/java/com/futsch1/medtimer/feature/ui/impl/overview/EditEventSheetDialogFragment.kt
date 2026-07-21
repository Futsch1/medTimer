package com.futsch1.medtimer.feature.ui.impl.overview

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
import com.futsch1.medtimer.core.common.helpers.DatePickerDialogFactory
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderTime
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.ui.impl.R
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
import java.time.LocalDate
import javax.inject.Inject

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

    @Inject
    lateinit var timePickerDialogFactory: TimePickerDialogFactory

    @Inject
    lateinit var datePickerDialogFactory: DatePickerDialogFactory

    @Inject
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

        setupTextFieldBindings()
        setupRemindedTimeDateFields()
        setupStatusDependentUI(editEventSheetDialog)
        setupToggleGroupListener()

        editEventSheetDialog.findViewById<MaterialToolbar>(R.id.editEventSideSheetToolbar)?.setNavigationOnClickListener {
            editEventSheetDialog.dismiss()
        }

        return editEventSheetDialog
    }

    private fun setupTextFieldBindings() {
        viewModel.medicineName.onEach { if (editEventName.text.toString() != it) editEventName.setText(it) }.launchIn(lifecycleScope)
        editEventName.doAfterTextChanged { viewModel.setMedicineName(it?.toString() ?: "") }

        viewModel.amount.onEach { if (editEventAmount.text.toString() != it) editEventAmount.setText(it) }.launchIn(lifecycleScope)
        editEventAmount.doAfterTextChanged { viewModel.setAmount(it?.toString() ?: "") }

        viewModel.notes.onEach { if (editEventNotes.text.toString() != it) editEventNotes.setText(it) }.launchIn(lifecycleScope)
        editEventNotes.doAfterTextChanged { viewModel.setNotes(it?.toString() ?: "") }
    }

    private fun setupRemindedTimeDateFields() {
        viewModel.remindedTimeString.onEach { if (editEventRemindedTimestamp.text.toString() != it) editEventRemindedTimestamp.setText(it) }
            .launchIn(lifecycleScope)
        setupTimePicker(editEventRemindedTimestamp, { viewModel.remindedMinutes }, { viewModel.remindedMinutes = it })

        viewModel.remindedDateString.onEach { if (editEventRemindedDate.text.toString() != it) editEventRemindedDate.setText(it) }.launchIn(lifecycleScope)
        setupDatePicker(editEventRemindedDate, { viewModel.remindedDate }, { viewModel.remindedDate = it })
    }

    private fun setupStatusDependentUI(editEventSheetDialog: AppCompatDialog) {
        viewModel.reminderStatus.filterNotNull().take(1).onEach { status ->
            configureTakenText(editEventSheetDialog, status)
            if (status != ReminderEvent.ReminderStatus.RAISED) {
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
    }

    private fun setupToggleGroupListener() {
        editEventToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.status = if (checkedId == R.id.takenToggleButton)
                    ReminderEvent.ReminderStatus.TAKEN
                else
                    ReminderEvent.ReminderStatus.SKIPPED
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.updateEvent()
        super.onDismiss(dialog)
    }

    private fun setupToggleGroup(status: ReminderEvent.ReminderStatus) {
        when (status) {
            ReminderEvent.ReminderStatus.TAKEN -> editEventToggleGroup.check(R.id.takenToggleButton)
            ReminderEvent.ReminderStatus.SKIPPED, ReminderEvent.ReminderStatus.RAISED -> editEventToggleGroup.check(R.id.skippedToggleButton)
            else -> editEventToggleGroup.visibility = View.GONE
        }
    }

    private fun setupTimePicker(editText: EditText, getMinutes: () -> Int, onTimePicked: (Int) -> Unit) {
        editText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) return@OnFocusChangeListener
            val startMinutes = getMinutes().takeIf { it >= 0 } ?: ReminderTime.DEFAULT_TIME
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

    private fun configureTakenText(dialog: AppCompatDialog, status: ReminderEvent.ReminderStatus) {
        val takenText = dialog.requireViewById<TextView>(R.id.takenText)
        when (status) {
            ReminderEvent.ReminderStatus.TAKEN -> takenText.setText(com.futsch1.medtimer.core.ui.R.string.taken)
            ReminderEvent.ReminderStatus.SKIPPED -> takenText.setText(com.futsch1.medtimer.core.ui.R.string.skipped)
            ReminderEvent.ReminderStatus.ACKNOWLEDGED -> {
                takenText.setText(com.futsch1.medtimer.core.ui.R.string.acknowledged)
                dialog.findViewById<TextInputLayout>(R.id.editEventAmountLayout)?.hint = ""
            }

            else -> takenText.visibility = View.GONE
        }
    }
}
