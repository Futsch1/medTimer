package com.futsch1.medtimer;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.futsch1.medtimer.database.MedicineRepository;
import com.futsch1.medtimer.helpers.PathHelper;
import com.futsch1.medtimer.reminders.ReminderProcessor;
import com.takisoft.preferencex.PreferenceFragmentCompat;
import com.takisoft.preferencex.RingtonePreference;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.TimeZone;

public class PreferencesFragment extends PreferenceFragmentCompat {
    public static final String EXACT_REMINDERS = "exact_reminders";

    private MedicineViewModel medicineViewModel;
    private HandlerThread backgroundThread;

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        setupVersion();
        setupAppURL();
        setupClearEvents();
        setupShowNotifications();
        setupExactReminders();
        setupExport();
        setupGenerateTestData();
        setupNotificationTone();
    }

    private void setupVersion() {
        Preference preference = getPreferenceScreen().findPreference("version");
        if (preference != null) {
            preference.setTitle(getString(R.string.version, BuildConfig.VERSION_NAME));
        }
    }

    private void setupAppURL() {
        Preference preference = getPreferenceScreen().findPreference("app_url");
        if (preference != null) {
            preference.setOnPreferenceClickListener(preference12 -> {
                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Futsch1/medTimer"));
                startActivity(myIntent);
                return true;
            });
        }
    }

    private void setupClearEvents() {
        medicineViewModel = new ViewModelProvider(this).get(MedicineViewModel.class);

        Preference preference = getPreferenceScreen().findPreference("clear_events");
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

    private void setupShowNotifications() {
        if (ActivityCompat.checkSelfPermission(requireContext(), POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Preference preference = getPreferenceScreen().findPreference("show_notification");
            if (preference != null) {
                preference.setEnabled(false);
                preference.setSummary(R.string.permission_not_granted);
            }
        }
    }

    private void setupExactReminders() {
        Preference preference = getPreferenceScreen().findPreference(EXACT_REMINDERS);
        if (preference != null) {
            preference.setOnPreferenceChangeListener((preference13, newValue) -> {
                if ((Boolean) newValue) {
                    AlarmManager alarmManager = requireContext().getSystemService(AlarmManager.class);
                    if (!alarmManager.canScheduleExactAlarms()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(R.string.enable_alarm_dialog).
                                setPositiveButton(R.string.ok, (dialog, id) -> {
                                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                    requireContext().startActivity(intent);
                                }).
                                setNegativeButton(R.string.cancel, (dialog, id) -> {
                                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putBoolean(EXACT_REMINDERS, false).apply();
                                    setPreferenceScreen(null);
                                    addPreferencesFromResource(R.xml.root_preferences);
                                });
                        AlertDialog d = builder.create();
                        d.show();
                    }
                }
                return true;
            });
        }
    }

    void setupExport() {
        backgroundThread = new HandlerThread("Export");
        backgroundThread.start();

        Preference preference = getPreferenceScreen().findPreference("export");
        if (preference != null) {
            preference.setOnPreferenceClickListener(preference1 -> {
                final Handler handler = new Handler(backgroundThread.getLooper());
                handler.post(() -> {
                    File csvFile = new File(requireContext().getCacheDir(), PathHelper.getExportFilename());
                    MedicineRepository medicineRepository = new MedicineRepository((Application) requireContext().getApplicationContext());
                    CSVCreator csvCreator = new CSVCreator(medicineRepository.getAllReminderEvents(), requireContext(), TimeZone.getDefault().toZoneId());
                    try {
                        csvCreator.create(csvFile);

                        shareFile(csvFile);
                    } catch (IOException e) {
                        Log.e("Error", "IO exception creating file");
                    }
                });
                return true;
            });
        }
    }

    void setupGenerateTestData() {
        Preference preference = getPreferenceScreen().findPreference("generate_test_data");
        if (preference != null) {
            if (BuildConfig.DEBUG) {
                preference.setVisible(true);
                preference.setOnPreferenceClickListener(preference1 -> {
                    final Handler handler = new Handler(backgroundThread.getLooper());
                    handler.post(() -> {
                        GenerateTestData generateTestData = new GenerateTestData(medicineViewModel);
                        generateTestData.deleteAll();
                        generateTestData.generateTestMedicine();
                    });
                    return true;
                });
            } else {
                preference.setVisible(false);
            }
        }
    }

    private void setupNotificationTone() {
        RingtonePreference preference = getPreferenceScreen().findPreference("notification_ringtone");
        if (preference != null) {
            preference.setOnPreferenceChangeListener((preference1, newValue) -> {
                NotificationChannelManager.updateNotificationChannel(requireContext(), (Uri) newValue);
                return true;
            });
        }
    }

    private void shareFile(File csvFile) {
        Uri uri = FileProvider.getUriForFile(requireContext(), "com.futsch1.medtimer.fileprovider", csvFile);
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);

        intentShareFile.setDataAndType(uri, URLConnection.guessContentTypeFromName(csvFile.getName()));
        //Allow sharing apps to read the file Uri
        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //Pass the file Uri instead of the path
        intentShareFile.putExtra(Intent.EXTRA_STREAM,
                uri);
        startActivity(Intent.createChooser(intentShareFile, "Share File"));
    }

    @Override
    public void onResume() {
        super.onResume();
        SwitchPreference preference = getPreferenceScreen().findPreference(EXACT_REMINDERS);
        if (preference != null) {
            AlarmManager alarmManager = requireContext().getSystemService(AlarmManager.class);
            if (!alarmManager.canScheduleExactAlarms()) {
                preference.setChecked(false);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        backgroundThread.quitSafely();
    }
}