package com.futsch1.medtimer.preferences;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.futsch1.medtimer.R;

public class RepeatRemindersPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.repeat_reminders_preferences, rootKey);
    }
}
