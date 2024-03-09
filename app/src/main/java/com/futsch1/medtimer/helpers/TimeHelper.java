package com.futsch1.medtimer.helpers;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class TimeHelper {

    private TimeHelper() {
        // Intentionally empty
    }

    public static String minutesToTime(long minutes) {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(LocalTime.of((int) (minutes / 60), (int) (minutes % 60)));
    }

    public static String toLocalizedTimeString(long timeStamp, ZoneId zoneId) {
        Instant remindedTime = Instant.ofEpochSecond(timeStamp);
        ZonedDateTime zonedDateTime = remindedTime.atZone(zoneId);

        return String.format("%s %s",
                zonedDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
                zonedDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)));
    }
}
