package com.futsch1.medtimer.helpers;

import android.text.format.DateFormat;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

public class TimeHelper {

    private TimeHelper() {
        // Intentionally empty
    }

    public static String minutesToTimeString(long minutes) {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(LocalTime.of((int) (minutes / 60), (int) (minutes % 60)));
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
