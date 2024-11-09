package com.futsch1.medtimer;

import android.os.Bundle;

import com.takisoft.preferencex.PreferenceFragmentCompat;

public class RepeatRemindersPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.repeat_reminders_preferences, rootKey);
    }
}
