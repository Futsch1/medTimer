package com.futsch1.medtimer;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferencesFragment extends PreferenceFragmentCompat {

    private MedicineViewModel medicineViewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        Preference preference = getPreferenceScreen().findPreference("version");
        if (preference != null) {
            preference.setTitle(getString(R.string.version, BuildConfig.VERSION_NAME));
        }

        preference = getPreferenceScreen().findPreference("clear_events");
        if (preference != null) {
            preference.setOnPreferenceClickListener(preference1 -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.confirm);
                builder.setMessage(R.string.are_you_sure_delete_events);
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> medicineViewModel.deleteReminderEvents());
                builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                });
                builder.show();
                ReminderProcessor.requestReschedule(requireContext());
                return true;
            });
        }
    }
}