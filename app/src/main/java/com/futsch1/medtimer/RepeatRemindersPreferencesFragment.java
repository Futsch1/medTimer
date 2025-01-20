package com.futsch1.medtimer;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class RepeatRemindersPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.repeat_reminders_preferences, rootKey);
    }
}
