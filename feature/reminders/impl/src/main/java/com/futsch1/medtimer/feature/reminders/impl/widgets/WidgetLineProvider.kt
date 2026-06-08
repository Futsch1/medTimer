package com.futsch1.medtimer.feature.reminders.impl.widgets

import android.text.Spanned

fun interface WidgetLineProvider {
    fun getWidgetLine(
        line: Int,
        isShort: Boolean
    ): Spanned
}
