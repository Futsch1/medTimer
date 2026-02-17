package com.futsch1.medtimer;

import static com.futsch1.medtimer.helpers.SafeHelperKt.safeStartActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.view.MenuCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.exporters.CSVEventExport;
import com.futsch1.medtimer.exporters.CSVMedicineExport;
import com.futsch1.medtimer.exporters.Export;
import com.futsch1.medtimer.exporters.PDFEventExport;
import com.futsch1.medtimer.exporters.PDFMedicineExport;
import com.futsch1.medtimer.helpers.EntityEditOptionsMenu;
import com.futsch1.medtimer.helpers.FileHelper;
import com.futsch1.medtimer.helpers.PathHelper;
import com.futsch1.medtimer.helpers.SimpleIdlingResource;
import com.futsch1.medtimer.medicine.tags.TagDataFromPreferences;
import com.futsch1.medtimer.medicine.tags.TagsFragment;
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;

public class OptionsMenu implements EntityEditOptionsMenu {
    private final Context context;
    private final Fragment fragment;
    private final MedicineViewModel medicineViewModel;
    private final NavController navController;
    private final HandlerThread backgroundThread;
    private final ActivityResultLauncher<Intent> openFileLauncher;
    private final boolean hideFilter;
    private final SimpleIdlingResource idlingResource;
    private Menu menu;
    private BackupManager backupManager;

    public OptionsMenu(Fragment fragment, MedicineViewModel medicineViewModel, NavController navController, boolean hideFilter) {
        this.fragment = fragment;
        this.context = fragment.requireContext();
        this.medicineViewModel = medicineViewModel;
        this.navController = navController;
        this.hideFilter = hideFilter;
        this.idlingResource = new SimpleIdlingResource("OptionsMenu_" + fragment.getClass().getName());
        this.openFileLauncher = fragment.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                this.fileSelected(result.getData().getData());
            }
        });
        backgroundThread = new HandlerThread("Export");
        backgroundThread.start();
        idlingResource.setIdle();
    }

    public void fileSelected(Uri data) {
        new Handler(backgroundThread.getLooper()).post(() -> backupManager.fileSelected(data));
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.main, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        enableOptionalIcons(menu);

        this.backupManager = new BackupManager(context, fragment.getParentFragmentManager(), menu, medicineViewModel, openFileLauncher);
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
    public static void enableOptionalIcons(@NonNull Menu menu) {
        if (menu instanceof MenuBuilder) {
            try {
                Method m = menu.getClass().getDeclaredMethod(
                        "setOptionalIconsVisible", Boolean.TYPE);
                m.invoke(menu, true);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // Intentionally empty
            }
        }
    }

    private void setupSettings() {
        MenuItem item = menu.findItem(R.id.settings);
        item.setOnMenuItemClickListener(menuItem -> {
            try {
                navController.navigate(R.id.action_global_preferencesFragment);
                return true;
            } catch (IllegalStateException e) {
                return false;
            }
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
            safeStartActivity(context, myIntent);
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
                ReminderProcessorBroadcastReceiver.requestScheduleNextNotification(context);
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
            handler.post(() -> eventExport(true));
            return true;
        });
        item = menu.findItem(R.id.export_pdf);
        item.setOnMenuItemClickListener(menuItem -> {
            handler.post(() -> eventExport(false));
            return true;
        });
        item = menu.findItem(R.id.export_medicine_csv);
        item.setOnMenuItemClickListener(menuItem -> {
            handler.post(() -> medicineExport(true));
            return true;
        });
        item = menu.findItem(R.id.export_medicine_pdf);
        item.setOnMenuItemClickListener(menuItem -> {
            handler.post(() -> medicineExport(false));
            return true;
        });
    }

    void setupGenerateTestData() {
        MenuItem item = menu.findItem(R.id.generate_test_data);
        MenuItem itemWithEvents = menu.findItem(R.id.generate_test_data_and_events);
        if (BuildConfig.DEBUG) {
            itemWithEvents.setVisible(true);
            item.setVisible(true);
            MenuItem.OnMenuItemClickListener menuItemClickListener = menuItem -> {
                idlingResource.setBusy();
                final Handler handler = new Handler(backgroundThread.getLooper());
                handler.post(() -> {
                    medicineViewModel.medicineRepository.deleteAll();
                    GenerateTestData generateTestData = new GenerateTestData(medicineViewModel, menuItem == itemWithEvents);
                    generateTestData.generateTestMedicine();
                    ReminderProcessorBroadcastReceiver.requestScheduleNextNotification(context);
                    idlingResource.setIdle();
                });
                return true;
            };
            item.setOnMenuItemClickListener(menuItemClickListener);
            itemWithEvents.setOnMenuItemClickListener(menuItemClickListener);
        } else {
            item.setVisible(false);
            itemWithEvents.setVisible(false);
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
                    try {
                        fragment.requireActivity().runOnUiThread(this::setupTagFilter);
                    } catch (IllegalStateException e) {
                        // Intentionally empty, do nothing
                    }
                } else {
                    medicineViewModel.getValidTagIds().postValue(new HashSet<>());
                }
            });
        }
    }

    private void eventExport(boolean isCSV) {
        if (medicineViewModel.tagFilterActive()) {
            Toast.makeText(context, R.string.tag_filter_active, Toast.LENGTH_LONG).show();
        }
        List<ReminderEvent> reminderEvents = medicineViewModel.filterEvents(medicineViewModel.medicineRepository.getAllReminderEventsWithoutDeletedAndAcknowledged());
        Export exporter = isCSV ? new CSVEventExport(reminderEvents, fragment.getParentFragmentManager(), context) : new PDFEventExport(reminderEvents, fragment.getParentFragmentManager(), context);
        export(exporter);
    }

    private void medicineExport(boolean isCSV) {
        if (medicineViewModel.tagFilterActive()) {
            Toast.makeText(context, R.string.tag_filter_active, Toast.LENGTH_LONG).show();
        }
        List<FullMedicine> medicines = medicineViewModel.filterMedicines(medicineViewModel.medicineRepository.getMedicines());
        Export exporter = isCSV ? new CSVMedicineExport(medicines, fragment.getParentFragmentManager(), context) : new PDFMedicineExport(medicines, fragment.getParentFragmentManager(), context);
        export(exporter);
    }

    private void setupTagFilter() {
        if (fragment.getView() == null) {
            return;
        }

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

    private void export(Export export) {
        File csvFile = new File(context.getCacheDir(), PathHelper.getExportFilename(export));
        try {
            export.export(csvFile);
            FileHelper.shareFile(context, csvFile);
        } catch (Export.ExporterException e) {
            Toast.makeText(context, R.string.export_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }

    public void onDestroy() {
        backgroundThread.quit();
        idlingResource.destroy();
    }
}
