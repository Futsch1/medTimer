package com.futsch1.medtimer.feature.reminders.widgets

import android.text.Spanned

fun interface WidgetLineProvider {

    @SuppressWarnings("kotlin:S6318")
    suspend fun prepareForUpdate() {
        // No-op by default
    }

    fun getWidgetLine(
        line: Int,
        isShort: Boolean
    ): Spanned
}
