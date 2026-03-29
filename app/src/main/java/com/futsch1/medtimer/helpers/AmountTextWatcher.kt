package com.futsch1.medtimer.helpers

import android.text.Editable
import android.text.TextWatcher
import com.futsch1.medtimer.R
import com.google.android.material.textfield.TextInputEditText

class AmountTextWatcher(val textEditInputEditText: TextInputEditText) : TextWatcher {
    override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
    ) {
        // Intentionally empty
    }

    override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) {
        // Intentionally empty
    }

    override fun afterTextChanged(s: Editable?) {
        if (MedicineHelper.parseAmount(s.toString()) == null) {
            textEditInputEditText.error =
                textEditInputEditText.context.getString(R.string.invalid_amount)
        }
    }

}