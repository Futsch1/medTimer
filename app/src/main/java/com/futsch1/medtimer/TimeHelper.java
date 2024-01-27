package com.futsch1.medtimer;

import java.util.Locale;

public class TimeHelper {
    public static String minutesToTime(long minutes) {
        return String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60);
    }
}
