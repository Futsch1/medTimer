package com.futsch1.medtimer.statistics

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.preferences.PreferencesNames.ANALYSIS_DAYS

class AnalysisDays(context: Context) {
    companion object {
        private const val DEFAULT_DAYS = 7
    }

    private val sharedPref: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    private val analysisDaysValues: IntArray =
        context.resources.getIntArray(R.array.analysis_days_values)

    var position: Int
        get() = analysisDaysValues.indexOf(this.days)
        set(position) {
            sharedPref.edit { putInt(ANALYSIS_DAYS, position) }
        }

    val days: Int
        get() {
            val days = sharedPref.getInt(ANALYSIS_DAYS, analysisDaysValues.indexOf(DEFAULT_DAYS))
                .coerceIn(0, analysisDaysValues.size - 1)
            return this.analysisDaysValues[days]
        }
}
