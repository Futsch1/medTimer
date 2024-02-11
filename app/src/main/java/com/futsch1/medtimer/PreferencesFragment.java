package com.futsch1.medtimer;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.helpers.PathHelper;
import com.futsch1.medtimer.reminders.ReminderProcessor;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

public class PreferencesFragment extends PreferenceFragmentCompat {

    private MedicineViewModel medicineViewModel;
    private HandlerThread backgroundThread;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        Preference preference = getPreferenceScreen().findPreference("version");
        if (preference != null) {
            preference.setTitle(getString(R.string.version, BuildConfig.VERSION_NAME));
        }
        preference = getPreferenceScreen().findPreference("app_url");
        if (preference != null) {
            preference.setOnPreferenceClickListener(preference12 -> {
                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Futsch1/medTimer"));
                startActivity(myIntent);
                return true;
            });
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

        backgroundThread = new HandlerThread("Export");
        backgroundThread.start();
        preference = getPreferenceScreen().findPreference("export");
        if (preference != null) {
            preference.setOnPreferenceClickListener(preference1 -> {
                final Handler handler = new Handler(backgroundThread.getLooper());
                handler.post(() -> {
                    Intent intentShareFile = new Intent(Intent.ACTION_SEND);

                    File csvFile = new File(requireContext().getCacheDir(), PathHelper.getExportFilename());
                    MedicineRepository medicineRepository = new MedicineRepository((Application) requireContext().getApplicationContext());
                    CSVCreator csvCreator = new CSVCreator(medicineRepository.getAllReminderEvents(), requireContext());
                    try {
                        csvCreator.create(csvFile);

                        Uri uri = FileProvider.getUriForFile(requireContext(), "com.futsch1.medtimer.fileprovider", csvFile);

                        intentShareFile.setDataAndType(uri, URLConnection.guessContentTypeFromName(csvFile.getName()));
                        //Allow sharing apps to read the file Uri
                        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        //Pass the file Uri instead of the path
                        intentShareFile.putExtra(Intent.EXTRA_STREAM,
                                uri);
                        startActivity(Intent.createChooser(intentShareFile, "Share File"));
                    } catch (IOException e) {
                        Log.e("Error", "IO exception creating file");
                    }
                });
                return true;
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        backgroundThread.quitSafely();
    }
}