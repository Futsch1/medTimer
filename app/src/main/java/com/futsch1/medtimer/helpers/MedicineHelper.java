package com.futsch1.medtimer.helpers;

import android.annotation.SuppressLint;
import android.content.Context;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.Medicine;

import java.text.NumberFormat;
import java.text.ParseException;
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
            return medicine.name + " (" + context.getString(R.string.medicine_stock_string, formatAmount(medicine.amount), medicine.amount <= medicine.outOfStockReminderThreshold ? " ⚠)" : ")");
        } else {
            return medicine.name;
        }
    }

    public static String formatAmount(double amount) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(amount);
    }

    public static double parseAmount(String amount) throws ParseException {
        Number n = NumberFormat.getNumberInstance().parse(amount);
        return n == null ? Double.NaN : n.doubleValue();
    }
}
