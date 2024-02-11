package com.futsch1.medtimer.helpers;

import java.time.Instant;

public class PathHelper {
    private static final String ReservedChars = "[\\[|?*<\":>+/'\\]]";

    public static String getExportFilename() {
        String fileName = String.format("medTimer_export_%s.csv", Instant.now().toString());
        return fileName.replaceAll(ReservedChars, "_");
    }
}
