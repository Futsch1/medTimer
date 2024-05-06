package com.futsch1.medtimer.helpers;

import android.content.Context;

import com.futsch1.medtimer.R;

import java.util.ArrayList;
import java.util.List;

public class TableHelper {
    private TableHelper() {
        // Intended empty
    }

    public static List<String> getTableHeaders(Context context) {
        ArrayList<String> header = new ArrayList<>();
        final int[] headerTexts = {R.string.time, R.string.name, R.string.dosage, R.string.taken};
        for (int headerText : headerTexts) {
            header.add(context.getString(headerText));
        }
        return header;
    }
}
