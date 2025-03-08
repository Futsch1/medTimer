package com.futsch1.medtimer.medicine.dialogs

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.rarepebble.colorpicker.ColorPickerView

class ColorPickerDialog(
    val context: Context,
    val activity: FragmentActivity,
    val color: Int
) {
    private val dialog: Dialog = Dialog(context)
    private val colorPickerView = ColorPickerView(context)

    init {
        colorPickerView.setColor(color)
        colorPickerView.showAlpha(false)
        colorPickerView.showHex(true)
        colorPickerView.showPreview(false)

        dialog.setContentView(colorPickerView)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.show()
    }
}