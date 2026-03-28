package com.futsch1.medtimer.helpers

import android.view.View
import android.view.inputmethod.InputMethodManager

fun View.showSoftKeyboard() {
    if (requestFocus()) {
        val imm = this.context.getSystemService(InputMethodManager::class.java) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}