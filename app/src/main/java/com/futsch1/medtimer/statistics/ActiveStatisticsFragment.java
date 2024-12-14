package com.futsch1.medtimer.statistics;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class ActiveStatisticsFragment {
    private static final int DEFAULT_FRAGMENT = StatisticFragmentType.CHARTS.ordinal();
    private final SharedPreferences sharedPref;

    public ActiveStatisticsFragment(Context context) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public StatisticFragmentType getActiveFragment() {
        return StatisticFragmentType.values()[sharedPref.getInt("active_statistics_fragment", DEFAULT_FRAGMENT)];
    }

    public void setActiveFragment(StatisticFragmentType fragment) {
        sharedPref.edit().putInt("active_statistics_fragment", fragment.ordinal()).apply();
    }

    public enum StatisticFragmentType {
        CHARTS,
        TABLE,
        CALENDAR
    }
}
