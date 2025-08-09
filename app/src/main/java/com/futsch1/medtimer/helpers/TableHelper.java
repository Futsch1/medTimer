package com.futsch1.medtimer.helpers;

import android.content.Context;

import com.futsch1.medtimer.R;

import java.util.Arrays;
import java.util.List;

public class TableHelper {
    private TableHelper() {
        // Intended empty
    }

    public static List<String> getTableHeadersForEventExport(Context context) {
        final String[] headerTexts = {
                context.getString(R.string.reminded),
                context.getString(R.string.name),
                context.getString(R.string.dosage),
                context.getString(R.string.taken),
                context.getString(R.string.tags),
                context.getString(R.string.interval),
                context.getString(R.string.notes),
                context.getString(R.string.reminded) + " (ISO 8601)",
                context.getString(R.string.taken) + " (ISO 8601)"};
        return Arrays.asList(headerTexts);
    }

    public static List<String> getTableHeadersForMedicationExport(Context context) {
        final String[] headerTexts = {
                context.getString(R.string.tab_medicine),
                context.getString(R.string.dosage),
                context.getString(R.string.time)
        };
        return Arrays.asList(headerTexts);
    }

    public static List<String> getTableHeadersForAnalysis(Context context) {
        final String[] headerTexts = {
                context.getString(R.string.taken),
                context.getString(R.string.name),
                context.getString(R.string.dosage),
                context.getString(R.string.reminded)};
        return Arrays.asList(headerTexts);
    }
}
