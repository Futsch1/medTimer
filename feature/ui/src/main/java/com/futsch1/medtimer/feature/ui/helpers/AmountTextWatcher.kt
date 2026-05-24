package com.futsch1.medtimer.feature.ui.helpers

import android.text.Editable
import android.text.TextWatcher
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.feature.ui.R
import com.google.android.material.textfield.TextInputEditText

class AmountTextWatcher(private val textEditInputEditText: TextInputEditText) : TextWatcher {
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
                textEditInputEditText.context.getString(com.futsch1.medtimer.core.ui.R.string.invalid_amount)
        }
    }

}
