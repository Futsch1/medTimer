package com.futsch1.medtimer.overview

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.reminderSummary
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.sidesheet.SideSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take

@AndroidEntryPoint
class ShowMedicineSheetDialogFragment : DialogFragment() {

    private val viewModel: ShowMedicineViewModel by viewModels()

    companion object {
        fun newInstance(reminderId: Int): ShowMedicineSheetDialogFragment {
            return ShowMedicineSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ShowMedicineViewModel.ARG_REMINDER_ID, reminderId)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AppCompatDialog {
        val context = requireContext()
        val dialog = if (context.resources.configuration.orientation == ORIENTATION_PORTRAIT) {
            BottomSheetDialog(context)
        } else {
            SideSheetDialog(context)
        }
        dialog.setContentView(R.layout.sheet_show_medicine)

        viewModel.uiState
            .filterIsInstance<ShowMedicineUiState.Loaded>()
            .take(1)
            .onEach { state -> bindData(dialog, state) }
            .launchIn(lifecycleScope)

        viewModel.uiState
            .filterIsInstance<ShowMedicineUiState.NotFound>()
            .take(1)
            .onEach { dismiss() }
            .launchIn(lifecycleScope)

        return dialog
    }

    private suspend fun bindData(
        dialog: AppCompatDialog,
        state: ShowMedicineUiState.Loaded
    ) {
        val context = requireContext()

        dialog.requireViewById<TextView>(R.id.medicineName).text =
            MedicineHelper.getMedicineNameWithStockTextInternal(
                context, state.userPreferences, state.fullMedicine
            )

        val datesTextView = dialog.requireViewById<TextView>(R.id.medicineDates)
        datesTextView.text = MedicineHelper.getDatesText(context, state.fullMedicine)
        checkIfTextElseGone(datesTextView)

        val notesTextView = dialog.requireViewById<TextView>(R.id.medicineNotes)
        notesTextView.text = state.fullMedicine.medicine.notes
        checkIfTextElseGone(notesTextView)

        val reminderTextView = dialog.requireViewById<TextView>(R.id.reminderText)
        reminderTextView.text = reminderSummary(state.reminder, context)
        reminderTextView.setCompoundDrawablesWithIntrinsicBounds(
            state.reminder.reminderType.icon, 0, 0, 0
        )
    }

    private fun checkIfTextElseGone(textView: TextView) {
        if (textView.text?.isEmpty() == true) {
            textView.visibility = View.GONE
        }
    }
}
