package com.futsch1.medtimer.medicine.dialogs

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import com.futsch1.medtimer.R
import com.google.android.material.textfield.TextInputEditText

class NotesDialog(
    val context: Context,
    val notes: String,
    val noteUpdatedCallback: (String) -> Unit
) {
    private val dialog: Dialog = Dialog(context)
    private val notesEditText: TextInputEditText

    init {
        dialog.setContentView(R.layout.dialog_fragment_notes)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        notesEditText = dialog.findViewById(R.id.notes)
        notesEditText.setText(notes)

        setupButtons()
        dialog.show()
    }

    private fun setupButtons() {
        dialog.findViewById<Button>(R.id.cancelSaveNotes)
            .setOnClickListener { _ -> dialog.dismiss() }
        dialog.findViewById<Button>(R.id.confirmSaveNotes).setOnClickListener {
            noteUpdatedCallback(notesEditText.getText().toString())
            dialog.dismiss()
        }
    }
}
