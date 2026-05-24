package com.futsch1.medtimer.feature.ui.helpers

import android.content.Context
import com.futsch1.medtimer.feature.ui.R

object TableHelper {
    fun getTableHeadersForEventExport(context: Context): List<String> = listOf(
        context.getString(com.futsch1.medtimer.core.ui.R.string.reminded),
        context.getString(com.futsch1.medtimer.core.ui.R.string.name),
        context.getString(com.futsch1.medtimer.core.ui.R.string.dosage),
        context.getString(com.futsch1.medtimer.core.ui.R.string.taken),
        context.getString(com.futsch1.medtimer.core.ui.R.string.tags),
        context.getString(com.futsch1.medtimer.core.ui.R.string.interval),
        context.getString(com.futsch1.medtimer.core.ui.R.string.notes),
        "${context.getString(com.futsch1.medtimer.core.ui.R.string.reminded)} (ISO 8601)",
        "${context.getString(com.futsch1.medtimer.core.ui.R.string.taken)} (ISO 8601)"
    )
}
