package com.futsch1.medtimer.helpers;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Date;

public class TimeHelper {

    private static final ZoneOffset EPOCH_OFFSET = ZoneId.systemDefault().getRules().getOffset(Instant.ofEpochSecond(0));

    private TimeHelper() {
        // Intentionally empty
    }

    public static String minutesToTimeString(Context context, long minutes) {
        Date date = localTimeToDate(LocalTime.of((int) (minutes / 60), (int) (minutes % 60)));
        return DateFormat.getTimeFormat(context).format(date);
    }

    private static Date localTimeToDate(LocalTime localTime) {
        return Date.from(localTime.atDate(LocalDate.ofEpochDay(0)).toInstant(EPOCH_OFFSET));
    }

    public static int timeStringToMinutes(String timeString) {
        try {
            return LocalTime.parse(timeString, DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)).toSecondOfDay() / 60;
        } catch (DateTimeParseException e) {
            return -1;
        }
    }

    public static String daysSinceEpochToDateString(long daysSinceEpoch) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(LocalDate.ofEpochDay(daysSinceEpoch));
    }

    public static @Nullable LocalDate dateStringToDate(String date) {
        try {
            return LocalDate.parse(date, DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static Instant instantFromTodayMinutes(int minutes) {
        LocalDate date = LocalDate.now();
        LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.of((minutes / 60), (minutes % 60)));
        return dateTime.toInstant(ZoneId.systemDefault().getRules().getOffset(dateTime));
    }

    public static long changeTimeStampMinutes(long timeStamp, int localMinutes) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeStamp), ZoneId.systemDefault());
        localDateTime = localDateTime.withHour(localMinutes / 60).withMinute(localMinutes % 60);
        return localDateTime.toEpochSecond(ZoneId.systemDefault().getRules().getOffset(localDateTime));
    }

    public static String toLocalizedDatetimeString(long timeStamp, ZoneId zoneId) {
        return toLocalizedDateString(timeStamp, zoneId) + " " + toLocalizedTimeString(timeStamp, zoneId);
    }

    public static String toLocalizedDateString(long timeStamp, ZoneId zoneId) {
        ZonedDateTime zonedDateTime = getZonedDateTime(timeStamp, zoneId);

        return zonedDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
    }

    public static String toLocalizedTimeString(long timeStamp, ZoneId zoneId) {
        ZonedDateTime zonedDateTime = getZonedDateTime(timeStamp, zoneId);

        return zonedDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
    }

    private static ZonedDateTime getZonedDateTime(long timeStamp, ZoneId zoneId) {
        Instant remindedTime = Instant.ofEpochSecond(timeStamp);
        return remindedTime.atZone(zoneId);
    }

    public interface TimePickerResult {
        void onTimeSelected(int minutes);
    }

    public static class TimePickerWrapper {
        final FragmentActivity activity;

        public TimePickerWrapper(FragmentActivity activity) {
            this.activity = activity;
        }

        public void show(int hourOfDay, int minute, TimePickerResult timePickerResult) {
            MaterialTimePicker timePickerDialog = new MaterialTimePicker.Builder()
                    .setTimeFormat(DateFormat.is24HourFormat(activity) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                    .setHour(hourOfDay)
                    .setMinute(minute)
                    .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                    .build();

            timePickerDialog.addOnPositiveButtonClickListener(view -> timePickerResult.onTimeSelected(timePickerDialog.getHour() * 60 + timePickerDialog.getMinute()));

            timePickerDialog.show(activity.getSupportFragmentManager(), "time_picker");
        }
    }
}
