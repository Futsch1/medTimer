package com.futsch1.medtimer.helpers

import android.content.Context
import com.futsch1.medtimer.R

object TableHelper {
    fun getTableHeadersForEventExport(context: Context): List<String> = listOf(
        context.getString(R.string.reminded),
        context.getString(R.string.name),
        context.getString(R.string.dosage),
        context.getString(R.string.taken),
        context.getString(R.string.tags),
        context.getString(R.string.interval),
        context.getString(R.string.notes),
        "${context.getString(R.string.reminded)} (ISO 8601)",
        "${context.getString(R.string.taken)} (ISO 8601)"
    )
}
