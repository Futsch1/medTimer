package com.futsch1.medtimer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.navigation.NavController;

import com.futsch1.medtimer.database.JSONBackup;
import com.futsch1.medtimer.database.JSONMedicineBackup;
import com.futsch1.medtimer.database.JSONReminderEventBackup;
import com.futsch1.medtimer.exporters.CSVExport;
import com.futsch1.medtimer.exporters.Exporter;
import com.futsch1.medtimer.exporters.PDFExport;
import com.futsch1.medtimer.helpers.FileHelper;
import com.futsch1.medtimer.helpers.PathHelper;
import com.futsch1.medtimer.reminders.ReminderProcessor;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.util.List;
import java.util.TimeZone;

public class OptionsMenu implements MenuProvider {
    private final Context context;
    private final MedicineViewModel medicineViewModel;
    private final HandlerThread backgroundThread;
    private final ActivityResultLauncher<Intent> openFileLauncher;
    private final NavController navController;
    private PendingFileOperation pendingFileOperation = PendingFileOperation.NONE;
    private Menu menu;

    public OptionsMenu(Context context, MedicineViewModel medicineViewModel, ActivityResultLauncher<Intent> openFileLauncher, NavController navController) {
        this.context = context;
        this.medicineViewModel = medicineViewModel;
        this.openFileLauncher = openFileLauncher;
        this.navController = navController;
        backgroundThread = new HandlerThread("Export");
        backgroundThread.start();
    }

    @SuppressLint("RestrictedApi")
    private static void enableOptionalIcons(@NonNull Menu menu) {
        if (menu instanceof MenuBuilder) {
            try {
                Method m = menu.getClass().getDeclaredMethod(
                        "setOptionalIconsVisible", Boolean.TYPE);
                m.invoke(menu, true);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                Log.e("Menu", "onMenuOpened", e);
            }
        }
    }

    public void onDestroy() {
        backgroundThread.quitSafely();
    }

    public void fileSelected(Uri data) {
        String json = FileHelper.readFromUri(data, context.getContentResolver());
        boolean restoreSuccessful = false;
        if (json != null) {
            if (pendingFileOperation == PendingFileOperation.MEDICINE) {
                restoreSuccessful = restoreBackup(json, new JSONMedicineBackup());
            } else if (pendingFileOperation == PendingFileOperation.REMINDER_EVENTS) {
                restoreSuccessful = restoreBackup(json, new JSONReminderEventBackup());
            }

            pendingFileOperation = PendingFileOperation.NONE;
        }

        new AlertDialog.Builder(context)
                .setMessage(restoreSuccessful ? R.string.restore_successful : R.string.restore_failed)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    // Intentionally empty
                })
                .show();
    }

    private <T> boolean restoreBackup(String json, JSONBackup<T> backup) {
        List<T> backupData = backup.parseBackup(json);
        if (backupData != null) {
            backup.applyBackup(backupData, medicineViewModel.medicineRepository);
            return true;
        }
        return false;
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.main, menu);
        menu.setGroupDividerEnabled(true);
        enableOptionalIcons(menu);

        this.menu = menu;
        setupSettings();
        setupVersion();
        setupAppURL();
        setupClearEvents();
        setupExport();
        setupGenerateTestData();
        setupBackup();
        setupEventsBackup();
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }

    private void setupEventsBackup() {
        Handler handler = new Handler(backgroundThread.getLooper());

        MenuItem item = menu.findItem(R.id.backup_events);
        item.setOnMenuItemClickListener(menuItem -> {
            handler.post(() ->
                    createBackup(new JSONReminderEventBackup(),
                            medicineViewModel.medicineRepository.getAllReminderEvents(),
                            "ReminderEvents"));
            return true;
        });

        item = menu.findItem(R.id.restore_events);
        item.setOnMenuItemClickListener(menuItem -> {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.restore)
                    .setMessage(R.string.restore_events_start)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, (dialog, which) -> openBackup(PendingFileOperation.REMINDER_EVENTS))
                    .setNegativeButton(R.string.cancel, (dialog, which) -> { // Intentionally left empty
                    }).show();
            return true;
        });
    }

    private <T> void createBackup(JSONBackup<T> jsonBackup, List<T> backupData, String backupType) {
        String jsonContent = jsonBackup.createBackup(medicineViewModel.medicineRepository.getVersion(),
                backupData);
        createAndSave(jsonContent, backupType);
    }

    private void setupSettings() {
        MenuItem item = menu.findItem(R.id.settings);
        item.setOnMenuItemClickListener(menuItem -> {
            navController.navigate(R.id.action_global_preferencesFragment);
            return true;
        });
    }

    private void setupVersion() {
        MenuItem item = menu.findItem(R.id.version);
        item.setTitle(context.getString(R.string.version, BuildConfig.VERSION_NAME));
    }

    private void setupAppURL() {
        MenuItem item = menu.findItem(R.id.app_url);
        item.setOnMenuItemClickListener(menuItem -> {
            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Futsch1/medTimer"));
            context.startActivity(myIntent);
            return true;
        });
    }

    private void setupClearEvents() {
        MenuItem item = menu.findItem(R.id.clear_events);
        item.setOnMenuItemClickListener(menuItem -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.confirm);
            builder.setMessage(R.string.are_you_sure_delete_events);
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> medicineViewModel.deleteReminderEvents());
            builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> { // Intentionally left empty
            });
            builder.show();
            ReminderProcessor.requestReschedule(context);
            return true;
        });
    }

    private void setupExport() {
        Handler handler = new Handler(backgroundThread.getLooper());

        MenuItem item = menu.findItem(R.id.export_csv);
        item.setOnMenuItemClickListener(menuItem -> {
            handler.post(() -> export(new CSVExport(medicineViewModel.medicineRepository.getAllReminderEvents(), context, TimeZone.getDefault().toZoneId())));
            return true;
        });
        item = menu.findItem(R.id.export_pdf);
        item.setOnMenuItemClickListener(menuItem -> {
            handler.post(() -> export(new PDFExport(medicineViewModel.medicineRepository.getAllReminderEvents(), context, TimeZone.getDefault().toZoneId())));
            return true;
        });
    }

    void setupGenerateTestData() {
        MenuItem item = menu.findItem(R.id.generate_test_data);
        if (BuildConfig.DEBUG) {
            item.setVisible(true);
            item.setOnMenuItemClickListener(menuItem -> {
                final Handler handler = new Handler(backgroundThread.getLooper());
                handler.post(() -> {
                    GenerateTestData generateTestData = new GenerateTestData(medicineViewModel);
                    generateTestData.deleteAll();
                    generateTestData.generateTestMedicine();
                });
                return true;
            });
        } else {
            item.setVisible(false);
        }
    }

    private void setupBackup() {
        Handler handler = new Handler(backgroundThread.getLooper());

        MenuItem item = menu.findItem(R.id.medicine_backup);
        item.setOnMenuItemClickListener(menuItem -> {
            handler.post(() ->
                    createBackup(new JSONMedicineBackup(),
                            medicineViewModel.medicineRepository.getMedicines(),
                            "Medicine"));
            return true;
        });

        item = menu.findItem(R.id.restore_medicine_backup);
        item.setOnMenuItemClickListener(menuItem -> {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.restore)
                    .setMessage(R.string.restore_start)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, (dialog, which) -> openBackup(PendingFileOperation.MEDICINE))
                    .setNegativeButton(R.string.cancel, (dialog, which) -> { // Intentionally left empty
                    }).show();
            return true;
        });
    }

    private void export(Exporter exporter) {
        File csvFile = new File(context.getCacheDir(), PathHelper.getExportFilename(exporter));
        try {
            exporter.export(csvFile);
            shareFile(csvFile);
        } catch (Exporter.ExporterException e) {
            Toast.makeText(context, R.string.export_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void createAndSave(String fileContent, String backupType) {
        File file = new File(context.getCacheDir(), PathHelper.getBackupFilename(backupType));
        if (FileHelper.saveToFile(file, fileContent)) {
            shareFile(file);
        } else {
            Toast.makeText(context, R.string.backup_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void openBackup(PendingFileOperation pendingFileOperation) {
        this.pendingFileOperation = pendingFileOperation;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        openFileLauncher.launch(intent);
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(context, "com.futsch1.medtimer.fileprovider", file);
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);

        intentShareFile.setDataAndType(uri, URLConnection.guessContentTypeFromName(file.getName()));
        //Allow sharing apps to read the file Uri
        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //Pass the file Uri instead of the path
        intentShareFile.putExtra(Intent.EXTRA_STREAM,
                uri);
        context.startActivity(Intent.createChooser(intentShareFile, "Share File"));
    }

    private enum PendingFileOperation {NONE, MEDICINE, REMINDER_EVENTS}
}
