package com.futsch1.medtimer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.futsch1.medtimer.database.JSONBackup;
import com.futsch1.medtimer.database.JSONMedicineBackup;
import com.futsch1.medtimer.database.JSONReminderEventBackup;
import com.futsch1.medtimer.helpers.FileHelper;
import com.futsch1.medtimer.helpers.PathHelper;

import java.io.File;
import java.util.List;

public class BackupManager {
    private final Context context;
    private final HandlerThread backgroundThread;
    private final Menu menu;
    private final MedicineViewModel medicineViewModel;
    private final ActivityResultLauncher<Intent> openFileLauncher;
    private PendingFileOperation pendingFileOperation = PendingFileOperation.NONE;


    public BackupManager(Context context, Menu menu, MedicineViewModel medicineViewModel, ActivityResultLauncher<Intent> openFileLauncher) {
        this.context = context;
        this.menu = menu;
        this.medicineViewModel = medicineViewModel;
        this.openFileLauncher = openFileLauncher;
        backgroundThread = new HandlerThread("Backup");
        backgroundThread.start();

        setupBackup();
        setupEventsBackup();
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

    private void setupEventsBackup() {
        Handler handler = new Handler(backgroundThread.getLooper());

        MenuItem item = menu.findItem(R.id.backup_events);
        item.setOnMenuItemClickListener(menuItem -> {
            handler.post(() ->
                    createBackup(new JSONReminderEventBackup(),
                            medicineViewModel.medicineRepository.getAllReminderEventsWithoutDeleted(),
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

    private void createAndSave(String fileContent, String backupType) {
        File file = new File(context.getCacheDir(), PathHelper.getBackupFilename(backupType));
        if (FileHelper.saveToFile(file, fileContent)) {
            FileHelper.shareFile(context, file);
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

    private enum PendingFileOperation {NONE, MEDICINE, REMINDER_EVENTS}
}
