package com.futsch1.medtimer.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.futsch1.medtimer.R

class RepeatRemindersPreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.repeat_reminders_preferences, rootKey)
    }
}
