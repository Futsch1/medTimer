package com.futsch1.medtimer;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.view.MenuCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.test.espresso.IdlingRegistry;

import com.futsch1.medtimer.exporters.CSVExport;
import com.futsch1.medtimer.exporters.Exporter;
import com.futsch1.medtimer.exporters.PDFExport;
import com.futsch1.medtimer.helpers.FileHelper;
import com.futsch1.medtimer.helpers.PathHelper;
import com.futsch1.medtimer.medicine.tags.TagDataFromPreferences;
import com.futsch1.medtimer.medicine.tags.TagsFragment;
import com.futsch1.medtimer.reminders.ReminderProcessor;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;

public class OptionsMenu implements MenuProvider {
    private final Context context;
    private final Fragment fragment;
    private final MedicineViewModel medicineViewModel;
    private final View view;
    private final HandlerThread backgroundThread;
    private final ActivityResultLauncher<Intent> openFileLauncher;
    private final boolean hideFilter;
    private Menu menu;
    private BackupManager backupManager;

    public OptionsMenu(Fragment fragment, MedicineViewModel medicineViewModel, View view, boolean hideFilter) {
        this.fragment = fragment;
        this.context = fragment.requireContext();
        this.medicineViewModel = medicineViewModel;
        this.view = view;
        this.hideFilter = hideFilter;
        this.openFileLauncher = fragment.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                this.fileSelected(result.getData().getData());
            }
        });
        backgroundThread = new HandlerThread("Export");
        backgroundThread.start();
    }

    public void fileSelected(Uri data) {
        backupManager.fileSelected(data);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.main, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        enableOptionalIcons(menu);

        this.backupManager = new BackupManager(context, menu, medicineViewModel, openFileLauncher);
        this.menu = menu;
        setupSettings();
        setupVersion();
        setupAppURL();
        setupClearEvents();
        setupExport();
        setupGenerateTestData();
        setupShowAppIntro();

        handleTagFilter();
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

    private void setupSettings() {
        MenuItem item = menu.findItem(R.id.settings);
        item.setOnMenuItemClickListener(menuItem -> {
            Navigation.findNavController(view).navigate(R.id.action_global_preferencesFragment);
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
            builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                medicineViewModel.medicineRepository.deleteReminderEvents();
                ReminderProcessor.requestReschedule(context);
            });
            builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> { // Intentionally left empty
            });
            builder.show();
            return true;
        });
    }

    private void setupExport() {
        Handler handler = new Handler(backgroundThread.getLooper());

        MenuItem item = menu.findItem(R.id.export_csv);
        item.setOnMenuItemClickListener(menuItem -> {
            handler.post(() -> export(new CSVExport(medicineViewModel.medicineRepository.getAllReminderEventsWithoutDeleted(), context)));
            return true;
        });
        item = menu.findItem(R.id.export_pdf);
        item.setOnMenuItemClickListener(menuItem -> {
            handler.post(() -> export(new PDFExport(medicineViewModel.medicineRepository.getAllReminderEventsWithoutDeleted(), context)));
            return true;
        });
    }

    void setupGenerateTestData() {
        MenuItem item = menu.findItem(R.id.generate_test_data);
        if (BuildConfig.DEBUG) {
            IdlingRegistry.getInstance().registerLooperAsIdlingResource(backgroundThread.getLooper());
            item.setVisible(true);
            item.setOnMenuItemClickListener(menuItem -> {
                final Handler handler = new Handler(backgroundThread.getLooper());
                handler.post(() -> {
                    Log.i("GenerateTestData", "Delete all data");
                    medicineViewModel.medicineRepository.deleteAll();
                    GenerateTestData generateTestData = new GenerateTestData(medicineViewModel);
                    Log.i("GenerateTestData", "Generate new medicine");
                    generateTestData.generateTestMedicine();
                    ReminderProcessor.requestReschedule(context);
                });
                return true;
            });
        } else {
            item.setVisible(false);
        }
    }

    private void setupShowAppIntro() {
        MenuItem item = menu.findItem(R.id.show_app_intro);
        if (BuildConfig.DEBUG) {
            item.setVisible(true);
            item.setOnMenuItemClickListener(menuItem -> {
                context.startActivity(new Intent(context, MedTimerAppIntro.class));
                return true;
            });
        } else {
            item.setVisible(false);
        }
    }

    private void handleTagFilter() {
        if (!hideFilter) {
            new Handler(backgroundThread.getLooper()).post(() -> {
                if (medicineViewModel.medicineRepository.hasTags()) {
                    fragment.requireActivity().runOnUiThread(this::setupTagFilter);
                } else {
                    medicineViewModel.getValidTagIds().postValue(new HashSet<>());
                }
            });
        }
    }

    private void export(Exporter exporter) {
        File csvFile = new File(context.getCacheDir(), PathHelper.getExportFilename(exporter));
        try {
            exporter.export(csvFile);
            FileHelper.shareFile(context, csvFile);
        } catch (Exporter.ExporterException e) {
            Toast.makeText(context, R.string.export_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void setupTagFilter() {
        MenuItem item = menu.findItem(R.id.tag_filter);
        item.setVisible(true);
        item.setOnMenuItemClickListener(menuItem -> {
            TagDataFromPreferences tagDataFromPreferences = new TagDataFromPreferences(fragment);
            DialogFragment dialog = new TagsFragment(tagDataFromPreferences);
            dialog.show(fragment.getParentFragmentManager(), "tags");
            return true;
        });
        medicineViewModel.getValidTagIds().observe(fragment.getViewLifecycleOwner(), validTagIds -> {
            if (validTagIds.isEmpty()) {
                item.setIcon(R.drawable.tag);
            } else {
                item.setIcon(R.drawable.tag_fill);
            }
        });
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }

    public void onDestroy() {
        backgroundThread.quit();
    }
}
