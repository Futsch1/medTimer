package com.futsch1.medtimer.medicine.dialogs

import android.app.Dialog
import android.view.ViewGroup
import android.widget.Button
import com.futsch1.medtimer.R
import com.futsch1.medtimer.medicine.EditMedicineFragment
import com.google.android.material.textfield.TextInputEditText

class NotesDialog(
    val editMedicineFragment: EditMedicineFragment
) {
    private val dialog: Dialog = Dialog(editMedicineFragment.requireContext())
    private val notesEditText: TextInputEditText

    init {
        dialog.setContentView(R.layout.dialog_fragment_notes)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        notesEditText = dialog.findViewById(R.id.notes)
        notesEditText.setText(editMedicineFragment.notes)

        setupButtons()
        dialog.show()
    }

    private fun setupButtons() {
        dialog.findViewById<Button>(R.id.cancelSaveNotes)
            .setOnClickListener { _ -> dialog.dismiss() }
        dialog.findViewById<Button>(R.id.confirmSaveNotes).setOnClickListener {
            editMedicineFragment.notes = notesEditText.getText().toString()
            dialog.dismiss()
        }
    }
}
