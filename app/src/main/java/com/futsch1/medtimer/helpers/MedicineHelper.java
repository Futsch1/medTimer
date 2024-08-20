package com.futsch1.medtimer.helpers;

import java.util.regex.Pattern;

public class MedicineHelper {
    private static final Pattern CYCLIC_COUNT = Pattern.compile(" (\\(\\d?/\\d?)\\)");

    private MedicineHelper() {
        // Intentionally empty
    }

    public static String normalizeMedicineName(String medicineName) {
        return CYCLIC_COUNT.matcher(medicineName).replaceAll("");
    }
}
