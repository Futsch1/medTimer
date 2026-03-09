package com.futsch1.medtimer.statistics

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R

class AnalysisDays(context: Context) {
    companion object {
        private const val PREFERENCE_KEY = "analysis_days"
        private const val DEFAULT_DAYS = 7
    }

    private val sharedPref: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    private val analysisDaysValues: IntArray =
        context.resources.getIntArray(R.array.analysis_days_values)

    var position: Int
        get() = analysisDaysValues.indexOf(this.days)
        set(position) {
            sharedPref.edit { putInt(PREFERENCE_KEY, position) }
        }

    val days: Int
        get() {
            val days = sharedPref.getInt(PREFERENCE_KEY, analysisDaysValues.indexOf(DEFAULT_DAYS))
                .coerceIn(0, analysisDaysValues.size - 1)
            return this.analysisDaysValues[days]
        }
}
