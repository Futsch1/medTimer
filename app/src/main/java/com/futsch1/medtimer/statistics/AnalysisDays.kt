package com.futsch1.medtimer.statistics

import android.content.Context
import com.futsch1.medtimer.R

object AnalysisDays {

    fun getPosition(context: Context, days: Int): Int {
        return context.resources.getIntArray(R.array.analysis_days_values).indexOf(days)
    }

    fun getDays(context: Context, position: Int): Int {
        return context.resources.getIntArray(R.array.analysis_days_values)[position]
    }
}

