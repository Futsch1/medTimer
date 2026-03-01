package com.futsch1.medtimer.statistics.domain

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class AnalysisDaysPreference(context: Context) {
    private companion object {
        const val PREF_KEY = "analysis_days"
    }

    private val sharedPref: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    var analysisDays: AnalysisDays
        get() {
            val ordinal = sharedPref.getInt(PREF_KEY, AnalysisDays.DEFAULT.ordinal)
            return AnalysisDays.entries.getOrElse(ordinal) { AnalysisDays.DEFAULT }
        }
        set(value) {
            sharedPref.edit { putInt(PREF_KEY, value.ordinal) }
        }
}