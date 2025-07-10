package com.futsch1.medtimer.helpers;

import com.futsch1.medtimer.exporters.Exporter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class PathHelper {
    private static final String RESERVED_CHARS = "[\\[|?*<\":>+/'\\],]";

    private PathHelper() {
        // Intentionally empty
    }

    public static String getExportFilename(Exporter exporter) {
        String fileName = String.format("MedTimer_Export_%s.%s", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")), exporter.getExtension());
        return fileName.replaceAll(RESERVED_CHARS, "_");
    }

    public static String getBackupFilename() {
        String fileName = String.format("MedTimer_Backup_%s.json", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        return fileName.replaceAll(RESERVED_CHARS, "_");
    }
}
