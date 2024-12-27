package com.futsch1.medtimer.helpers;

import android.annotation.SuppressLint;
import android.content.Context;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;

import java.util.regex.Pattern;

public class MedicineHelper {
    private static final Pattern CYCLIC_COUNT = Pattern.compile(" (\\(\\d?/\\d?)\\)");

    private MedicineHelper() {
        // Intentionally empty
    }

    public static String normalizeMedicineName(String medicineName) {
        return CYCLIC_COUNT.matcher(medicineName).replaceAll("");
    }

    @SuppressLint("DefaultLocale")
    public static String getMedicineNameWithStockText(Context context, Medicine medicine) {
        if (medicine.isStockManagementActive()) {
            return medicine.name + " (" + context.getString(R.string.medicine_stock_string, String.format("%d", medicine.amount), medicine.amount <= medicine.outOfStockReminderThreshold ? " ⚠)" : ")");
        } else {
            return medicine.name;
        }
    }
}
