package com.futsch1.medtimer.statistics;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.futsch1.medtimer.R;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AnalysisDays {
    private static final int DEFAULT_DAYS = 7;
    private final SharedPreferences sharedPref;
    private final List<Integer> analysisDaysValues;

    public AnalysisDays(Context context) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        analysisDaysValues = Arrays.stream(context.getResources().getStringArray(R.array.analysis_days_values)).map(Integer::valueOf).collect(Collectors.toList());
    }

    public int getPosition() {
        return analysisDaysValues.indexOf(getDays());
    }

    public void setPosition(int position) {
        sharedPref.edit().putInt("analysis_days", position).apply();
    }

    public int getDays() {
        return this.analysisDaysValues.get(sharedPref.getInt("analysis_days", analysisDaysValues.indexOf(DEFAULT_DAYS)));
    }
}
