package com.futsch1.medtimer.helpers;

import com.futsch1.medtimer.exporters.Export;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class PathHelper {
    private static final String RESERVED_CHARS = "[\\[|?*<\":>+/'\\],]";

    private PathHelper() {
        // Intentionally empty
    }

    public static String getExportFilename(Export export) {
        String fileName = String.format("MedTimer_%sExport_%s.%s", export.getType(), ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")), export.getExtension());
        return fileName.replaceAll(RESERVED_CHARS, "_");
    }

    public static String getBackupFilename() {
        String fileName = String.format("MedTimer_Backup_%s.json", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        return fileName.replaceAll(RESERVED_CHARS, "_");
    }
}
