package com.futsch1.medtimer.statistics.domain

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

class StatisticsTabPreference @Inject constructor(
    private val sharedPref: SharedPreferences,
) {
    private companion object {
        private val DEFAULT_TAB = StatisticsTabType.CHARTS.ordinal
        const val PREF_KEY = "active_statistics_fragment"
    }

    var activeFragment: StatisticsTabType
        get() = StatisticsTabType.entries[sharedPref.getInt(
            PREF_KEY,
            DEFAULT_TAB
        )]
        set(fragment) {
            sharedPref.edit { putInt(PREF_KEY, fragment.ordinal) }
        }
}
