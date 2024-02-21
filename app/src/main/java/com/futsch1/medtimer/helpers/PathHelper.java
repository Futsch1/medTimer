package com.futsch1.medtimer.helpers;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class PathHelper {
    private static final String RESERVED_CHARS = "[\\[|?*<\":>+/'\\],]";

    private PathHelper() {
        // Intentionally empty
    }

    public static String getExportFilename() {
        String fileName = String.format("MedTimer Export %s.csv", ZonedDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
        return fileName.replaceAll(RESERVED_CHARS, "_");
    }
}
