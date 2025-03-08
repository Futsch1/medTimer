package com.futsch1.medtimer.medicine.dialogs

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.R
import com.rarepebble.colorpicker.ColorPickerView

class ColorPickerDialog(
    val context: Context,
    val activity: FragmentActivity,
    val color: Int,
    val colorSelectedCallback: (Int) -> Unit
) {
    private val dialog: Dialog = Dialog(context)
    private val colorPickerView: ColorPickerView

    init {
        dialog.setContentView(R.layout.dialog_color_editor)
        colorPickerView = dialog.findViewById<ColorPickerView>(R.id.colorPickerView)
        colorPickerView.setColor(color)
        colorPickerView.showAlpha(false)
        colorPickerView.showHex(true)
        colorPickerView.showPreview(false)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupButtons()
        dialog.show()
    }

    private fun setupButtons() {
        dialog.findViewById<Button>(R.id.cancelSelectColor)
            .setOnClickListener { _ -> dialog.dismiss() }
        dialog.findViewById<Button>(R.id.confirmSelectColor).setOnClickListener {
            colorSelectedCallback(colorPickerView.color)
            dialog.dismiss()
        }
    }
}