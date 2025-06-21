package com.futsch1.medtimer.helpers

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.util.TypedValue
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import com.futsch1.medtimer.BuildConfig
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.math.roundToInt

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
        layout(textInputLayout)
        hint?.let(editText::setHint)
        editText.setSingleLine()
        editText.minimumHeight = dpToPx(context.resources, 48)
        editText.id = android.R.id.input
        initialText?.let(editText::setText)
        inputType?.let(editText::setInputType)

        textInputLayout.addView(editText)

        val builder = AlertDialog.Builder(context)
        builder.setView(textInputLayout)
        title?.let(builder::setTitle)
        builder.setPositiveButton(com.futsch1.medtimer.R.string.ok) { _: DialogInterface?, _: Int ->
            val e = editText.text
            if (e != null) {
                textSink?.consumeText(e.toString())
            }
        }
        builder.setNegativeButton(com.futsch1.medtimer.R.string.cancel) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
            cancelCallback?.cancel()
        }

        if (!BuildConfig.DEBUG) {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            editText.postDelayed({
                editText.requestFocus()
                inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }, 200)
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun layout(textInputLayout: TextInputLayout) {
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textInputLayout.layoutParams = layoutParams
        val padding = dpToPx(context.resources, 16)
        textInputLayout.setPadding(padding, padding, padding, padding)
    }

    @Suppress("SameParameterValue")
    private fun dpToPx(r: Resources, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            r.displayMetrics
        ).roundToInt()
    }
}
