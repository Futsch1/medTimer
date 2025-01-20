package com.futsch1.medtimer.helpers;

import android.content.Context;

import com.futsch1.medtimer.R;

import java.util.Arrays;
import java.util.List;

public class TableHelper {
    private TableHelper() {
        // Intended empty
    }

    public static List<String> getTableHeaders(Context context, boolean withIsoTimestamps) {
        final String[] headerTexts = {
                context.getString(R.string.time),
                context.getString(R.string.name),
                context.getString(R.string.dosage),
                context.getString(R.string.taken),
                context.getString(R.string.time) + " (ISO 8601)",
                context.getString(R.string.taken) + " (ISO 8601)"};
        List<String> names = Arrays.asList(headerTexts);
        if (!withIsoTimestamps) {
            return names.subList(0, names.size() - 2);
        } else {
            return names;
        }
    }
}
