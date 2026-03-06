package com.futsch1.medtimer.statistics.domain

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

class AnalysisDaysPreference @Inject constructor(
    private val sharedPref: SharedPreferences,
) {
    private companion object {
        const val PREF_KEY = "analysis_days"
    }

    var analysisDays: AnalysisDays
        get() {
            val ordinal = sharedPref.getInt(PREF_KEY, AnalysisDays.DEFAULT.ordinal)
            return AnalysisDays.entries.getOrElse(ordinal) { AnalysisDays.DEFAULT }
        }
        set(value) {
            sharedPref.edit { putInt(PREF_KEY, value.ordinal) }
        }
}
