package com.futsch1.medtimer.exporters;

import java.io.File;

public interface Exporter {
    void export(File file) throws ExporterException;

    String getExtension();

    class ExporterException extends Exception {
    }
}
