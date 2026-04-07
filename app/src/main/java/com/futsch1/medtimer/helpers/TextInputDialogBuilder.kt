package com.futsch1.medtimer.helpers

import android.content.Context
import android.content.DialogInterface
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.view.setPadding
import com.futsch1.medtimer.BuildConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.math.roundToInt

class TextInputDialogBuilder(
    private val context: Context
) {
    private var titleString: String? = null
    private var hasBeenShown = false

    @StringRes
    private var hint: Int? = null
    private var initialText: String? = null
    private var textSink: TextSink? = null
    private var cancelCallback: CancelCallback? = null
    private var inputType: Int? = null

    fun interface TextSink {
        fun consumeText(text: String)
    }

    fun interface CancelCallback {
        fun cancel()
    }

    fun title(@StringRes title: Int) = apply { this.titleString = context.resources.getString(title) }

    fun title(title: String) = apply { this.titleString = title }

    fun hint(@StringRes hint: Int) = apply { this.hint = hint }

    fun initialText(initialText: String?) = apply { this.initialText = initialText }

    fun textSink(textSink: TextSink) = apply { this.textSink = textSink }

    fun cancelCallback(cancelCallback: CancelCallback) =
        apply { this.cancelCallback = cancelCallback }

    fun inputType(inputType: Int) = apply { this.inputType = inputType }

    fun show() {
        if (hasBeenShown) {
            throw IllegalStateException("TextInputDialogBuilder can only be used once")
        }

        hasBeenShown = true

        val editText = TextInputEditText(context).apply {
            setSingleLine()
            showSoftInputOnFocus = true
            minimumHeight = context.resources.dpToPx(48f).roundToInt()
            id = android.R.id.input
            this@TextInputDialogBuilder.hint?.let { setHint(it) }
            this@TextInputDialogBuilder.initialText?.let { setText(it) }
            this@TextInputDialogBuilder.inputType?.let { setInputType(it) }

            if (!BuildConfig.DEBUG) {
                postDelayed({
                    this.showSoftKeyboard()
                }, 200)
            }
        }

        val textInputLayout = createTextInputLayout(context).apply {
            addView(editText)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(textInputLayout)
            .setPositiveButton(com.futsch1.medtimer.R.string.ok) { _: DialogInterface?, _: Int ->
                editText.text?.let { textSink?.consumeText(it.toString()) }
            }
            .setNegativeButton(com.futsch1.medtimer.R.string.cancel) { dialog: DialogInterface, _: Int ->
                cancelCallback?.cancel()
                dialog.dismiss()
            }
            .apply {
                titleString?.let { setTitle(it) }
            }.create()

        dialog.show()
    }

    private fun createTextInputLayout(context: Context): TextInputLayout {
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val padding = context.resources.dpToPx(16f).toInt()
        return TextInputLayout(context).apply {
            this.layoutParams = layoutParams
            setPadding(padding)
        }
    }
}
