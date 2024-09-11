package com.futsch1.medtimer.helpers;

import com.futsch1.medtimer.exporters.Exporter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class PathHelper {
    private static final String RESERVED_CHARS = "[\\[|?*<\":>+/'\\],]";

    private PathHelper() {
        // Intentionally empty
    }

    public static String getExportFilename(Exporter exporter) {
        String fileName = String.format("MedTimer Export %s.%s", ZonedDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)), exporter.getExtension());
        return fileName.replaceAll(RESERVED_CHARS, "_");
    }

    public static String getBackupFilename() {
        String fileName = String.format("MedTimer Backup %s.json", ZonedDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
        return fileName.replaceAll(RESERVED_CHARS, "_");
    }
}
