package com.futsch1.medtimer.statistics

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.preferences.PreferencesNames.ACTIVE_STATISTICS_FRAGMENT

class ActiveStatisticsFragment(context: Context) {

    companion object {
        private val DEFAULT_FRAGMENT = StatisticFragmentType.CHARTS.ordinal
    }

    private val sharedPref: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    var activeFragment: StatisticFragmentType
        get() = StatisticFragmentType.entries[sharedPref.getInt(
            ACTIVE_STATISTICS_FRAGMENT,
            DEFAULT_FRAGMENT
        )]
        set(fragment) {
            sharedPref.edit { putInt(ACTIVE_STATISTICS_FRAGMENT, fragment.ordinal) }
        }
}
