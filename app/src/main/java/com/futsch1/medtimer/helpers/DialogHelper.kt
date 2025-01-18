package com.futsch1.medtimer.helpers

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class DialogHelper(var context: Context) {
    var title: Int? = null
    var hint: Int? = null
    private var initialText: String? = null
    private var textSink: TextSink? = null
    private var cancelCallback: CancelCallback? = null
    private var inputType: Int? = null

    fun interface TextSink {
        fun consumeText(text: String?)
    }

    fun interface CancelCallback {
        fun cancel()
    }

    fun title(title: Int) = apply { this.title = title }
    fun hint(hint: Int) = apply { this.hint = hint }
    fun initialText(initialText: String?) = apply { this.initialText = initialText }
    fun textSink(textSink: TextSink) = apply { this.textSink = textSink }
    fun cancelCallback(cancelCallback: CancelCallback) =
        apply { this.cancelCallback = cancelCallback }

    fun inputType(inputType: Int) = apply { this.inputType = inputType }

    fun show() {
        val textInputLayout = TextInputLayout(context)
        val editText = TextInputEditText(context)
        editText.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        hint?.let(editText::setHint)
        editText.setSingleLine()
        editText.minimumHeight = dpToPx(context.resources, 48)
        editText.id = android.R.id.input
        initialText?.let(editText::setText)
        textInputLayout.addView(editText)
        inputType?.let(editText::setInputType)

        val builder = AlertDialog.Builder(context)
        builder.setView(textInputLayout)
        title?.let(builder::setTitle)
        builder.setPositiveButton(com.futsch1.medtimer.R.string.ok) { _: DialogInterface?, _: Int ->
            val e = editText.text
            if (e != null) {
                textSink?.consumeText(e.toString())
            }
        }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
            cancelCallback?.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    @Suppress("SameParameterValue")
    private fun dpToPx(r: Resources, dp: Int): Int {
        return Math.round(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                r.displayMetrics
            )
        )
    }
}
