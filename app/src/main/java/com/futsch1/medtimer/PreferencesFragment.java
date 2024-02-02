package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.RESCHEDULE_ACTION;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
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
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.confirm);
                    builder.setMessage(R.string.are_you_sure_delete_events);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> medicineViewModel.deleteReminderEvents());
                    builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    });
                    builder.show();
                    Intent intent = new Intent(RESCHEDULE_ACTION);
                    intent.setClass(requireContext(), ReminderProcessor.class);
                    requireContext().sendBroadcast(intent);
                    return true;
                }
            });
        }
    }
}