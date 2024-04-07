package com.futsch1.medtimer.helpers;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.format.DateFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class TimeHelper {

    private TimeHelper() {
        // Intentionally empty
    }

    public static String minutesToTime(long minutes) {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(LocalTime.of((int) (minutes / 60), (int) (minutes % 60)));
    }

    public static String daysSinceEpochToDate(long daysSinceEpoch) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(LocalDate.ofEpochDay(daysSinceEpoch));
    }

    public interface TimePickerResult {
        void onTimeSelected(int minutes);
    }

    public static class TimePickerWrapper {
        Context context;

        public TimePickerWrapper(Context context) {
            this.context = context;
        }

        public void show(int hourOfDay, int minute, TimePickerResult timePickerResult) {
            TimePickerDialog timePickerDialog = new TimePickerDialog(context, (view, hourOfDayLocal, minuteLocal) -> timePickerResult.onTimeSelected(hourOfDayLocal * 60 + minuteLocal), hourOfDay, minute, DateFormat.is24HourFormat(context));
            timePickerDialog.show();
        }
    }
}
