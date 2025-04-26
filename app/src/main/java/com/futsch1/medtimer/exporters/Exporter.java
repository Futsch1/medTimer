package com.futsch1.medtimer.exporters;

import androidx.fragment.app.FragmentManager;

import com.futsch1.medtimer.helpers.ProgressDialogFragment;

import java.io.File;

public abstract class Exporter {
    private final FragmentManager fragmentManager;

    Exporter(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public void export(File file) throws ExporterException {
        ProgressDialogFragment progressDialog = new ProgressDialogFragment();
        progressDialog.show(fragmentManager, "exporting");
        exportInternal(file);
        progressDialog.dismiss();
    }

    protected abstract void exportInternal(File file) throws ExporterException;

    public abstract String getExtension();

    public static class ExporterException extends Exception {
    }
}
