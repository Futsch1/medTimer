package com.futsch1.medtimer.statistics

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class ActiveStatisticsFragment(context: Context) {

    companion object {
        private const val PREFERENCE_KEY = "active_statistics_fragment"
        private val DEFAULT_FRAGMENT = StatisticFragmentType.CHARTS.ordinal
    }

    private val sharedPref: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    var activeFragment: StatisticFragmentType
        get() = StatisticFragmentType.entries[sharedPref.getInt(
            PREFERENCE_KEY,
            DEFAULT_FRAGMENT
        )]
        set(fragment) {
            sharedPref.edit { putInt(PREFERENCE_KEY, fragment.ordinal) }
        }

    enum class StatisticFragmentType {
        CHARTS,
        TABLE,
        CALENDAR
    }
}
