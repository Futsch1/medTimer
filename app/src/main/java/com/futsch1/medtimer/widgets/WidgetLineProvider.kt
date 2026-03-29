package com.futsch1.medtimer.widgets

import android.text.Spanned

fun interface WidgetLineProvider {
    fun getWidgetLine(
        line: Int,
        isShort: Boolean
    ): Spanned
}
