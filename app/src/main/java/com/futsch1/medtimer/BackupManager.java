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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.util.List;

public class BackupManager {
    private static final String MEDICINE_KEY = "medicines";
    private static final String EVENT_KEY = "events";
    private final Context context;
    private final HandlerThread backgroundThread;
    private final Menu menu;
    private final MedicineViewModel medicineViewModel;
    private final ActivityResultLauncher<Intent> openFileLauncher;


    public BackupManager(Context context, Menu menu, MedicineViewModel medicineViewModel, ActivityResultLauncher<Intent> openFileLauncher) {
        this.context = context;
        this.menu = menu;
        this.medicineViewModel = medicineViewModel;
        this.openFileLauncher = openFileLauncher;
        backgroundThread = new HandlerThread("Backup");
        backgroundThread.start();

        setupBackup();
    }

    private void setupBackup() {
        MenuItem item = menu.findItem(R.id.create_backup);
        item.setOnMenuItemClickListener(menuItem -> {
            selectBackupType();
            return true;
        });

        item = menu.findItem(R.id.restore_backup);
        item.setOnMenuItemClickListener(menuItem -> {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.restore)
                    .setMessage(R.string.restore_start)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, (dialog, which) -> openBackup())
                    .setNegativeButton(R.string.cancel, (dialog, which) -> { // Intentionally left empty
                    }).show();
            return true;
        });
    }

    private void selectBackupType() {
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(context);
        alertDialogBuilder.setTitle(R.string.backup);
        boolean[] checkedItems = new boolean[2];
        alertDialogBuilder.setMultiChoiceItems(R.array.backup_types, null, (dialog, which, isChecked) -> checkedItems[which] = isChecked);
        alertDialogBuilder.setPositiveButton(R.string.ok, (dialog, which) -> {
            Handler handler = new Handler(backgroundThread.getLooper());
            handler.post(() -> {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject jsonObject = new JsonObject();
                if (checkedItems[0]) {
                    jsonObject.add(MEDICINE_KEY, createBackup(new JSONMedicineBackup(),
                            medicineViewModel.medicineRepository.getMedicines()));
                }
                if (checkedItems[1]) {
                    jsonObject.add(EVENT_KEY, createBackup(new JSONReminderEventBackup(),
                            medicineViewModel.medicineRepository.getAllReminderEventsWithoutDeleted()));
                }
                createAndSave(gson.toJson(jsonObject));
            });
        });
        alertDialogBuilder.show();
    }

    private void openBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        openFileLauncher.launch(intent);
    }

    private <T> JsonElement createBackup(JSONBackup<T> jsonBackup, List<T> backupData) {
        return jsonBackup.createBackup(medicineViewModel.medicineRepository.getVersion(),
                backupData);
    }

    private void createAndSave(String fileContent) {
        File file = new File(context.getCacheDir(), PathHelper.getBackupFilename());
        if (FileHelper.saveToFile(file, fileContent)) {
            FileHelper.shareFile(context, file);
        } else {
            Toast.makeText(context, R.string.backup_failed, Toast.LENGTH_LONG).show();
        }
    }

    public void fileSelected(Uri data) {
        String json = FileHelper.readFromUri(data, context.getContentResolver());
        boolean restoreSuccessful = false;
        if (json != null) {
            try {
                JsonObject rootElement = JsonParser.parseString(json).getAsJsonObject();
                if (rootElement.has(MEDICINE_KEY)) {
                    restoreSuccessful = restoreBackup(rootElement.get(MEDICINE_KEY).toString(),
                            new JSONMedicineBackup());
                }
                if (rootElement.has(EVENT_KEY)) {
                    restoreSuccessful = restoreSuccessful && restoreBackup(rootElement.get(EVENT_KEY).toString(),
                            new JSONReminderEventBackup());
                }
            } catch (JsonSyntaxException e) {
                restoreSuccessful = false;
            }
        }

        if (!restoreSuccessful) {
            // Try legacy backup formats
            restoreSuccessful = restoreBackup(json, new JSONMedicineBackup()) || restoreBackup(json, new JSONReminderEventBackup());
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
}
