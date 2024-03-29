package com.futsch1.medtimer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.core.content.FileProvider;

import com.futsch1.medtimer.exporters.CSVExport;
import com.futsch1.medtimer.exporters.Exporter;
import com.futsch1.medtimer.exporters.PDFExport;
import com.futsch1.medtimer.helpers.PathHelper;
import com.futsch1.medtimer.reminders.ReminderProcessor;

import java.io.File;
import java.net.URLConnection;
import java.util.TimeZone;

public class OptionsMenu {
    private final Menu menu;
    private final Context context;
    private final MedicineViewModel medicineViewModel;
    private HandlerThread backgroundThread;

    public OptionsMenu(Context context, Menu menu, MedicineViewModel medicineViewModel) {
        this.menu = menu;
        this.context = context;
        this.medicineViewModel = medicineViewModel;

        setupSettings();
        setupVersion();
        setupAppURL();
        setupClearEvents();
        setupExport();
        setupGenerateTestData();
    }

    private void setupSettings() {
        MenuItem item = menu.findItem(R.id.settings);
        item.setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(context, PreferencesActivity.class);
            context.startActivity(intent);
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
            builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            });
            builder.show();
            ReminderProcessor.requestReschedule(context);
            return true;
        });
    }

    void setupExport() {
        backgroundThread = new HandlerThread("Export");
        backgroundThread.start();
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

    private void export(Exporter exporter) {
        File csvFile = new File(context.getCacheDir(), PathHelper.getExportFilename(exporter));
        try {
            exporter.export(csvFile);

            shareFile(csvFile);
        } catch (Exporter.ExporterException e) {
            Log.e("Error", "IO exception creating file");
        }
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

    public void onDestroy() {
        backgroundThread.quitSafely();
    }
}