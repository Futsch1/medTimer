package com.futsch1.medtimer.helpers;

import static com.futsch1.medtimer.preferences.PreferencesNames.USE_RELATIVE_DATE_TIME;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

public class TimeHelper {

    private static final ZoneOffset EPOCH_OFFSET = ZoneId.systemDefault().getRules().getOffset(Instant.ofEpochSecond(0));

    private TimeHelper() {
        // Intentionally empty
    }

    /**
     * @param context Context to extract time format
     * @param minutes Minutes since midnight
     * @return Time string in local format
     */
    public static String minutesToTimeString(Context context, long minutes) {
        try {
            Date date = localTimeToDate(LocalTime.of((int) (minutes / 60), (int) (minutes % 60)));
            return DateFormat.getTimeFormat(context).format(date);
        } catch (DateTimeException e) {
            return minutesToDurationString(minutes);
        }
    }

    /**
     * @param localTime Local time
     * @return Date of local time on epoch day 0
     */
    public static Date localTimeToDate(LocalTime localTime) {
        return Date.from(localTime.atDate(LocalDate.ofEpochDay(0)).toInstant(EPOCH_OFFSET));
    }

    /**
     * @param minutes Minutes since midnight
     * @return Time string in local format
     */
    @SuppressLint("DefaultLocale")
    public static String minutesToDurationString(long minutes) {
        return String.format("%d:%02d", minutes / 60, minutes % 60);
    }

    /**
     * @param timeString Time string in local format
     * @return Minutes since midnight
     */
    public static int durationStringToMinutes(String timeString) {
        try {
            TemporalAccessor accessor = DateTimeFormatter.ofPattern("H:mm").parse(timeString);
            return accessor.get(ChronoField.HOUR_OF_DAY) * 60 + accessor.get(ChronoField.MINUTE_OF_HOUR);
        } catch (DateTimeParseException e) {
            return -1;
        }
    }

    /**
     * @param secondsSinceEpoch Seconds since epoch
     * @param zoneId            Zone id
     * @return Local date
     */
    public static LocalDate secondsSinceEpochToLocalDate(long secondsSinceEpoch, ZoneId zoneId) {
        return Instant.ofEpochSecond(secondsSinceEpoch).atZone(zoneId).toLocalDate();
    }

    /**
     * @param context        Context to extract date format
     * @param daysSinceEpoch Days since epoch
     * @return Date string in local format
     */
    public static String daysSinceEpochToDateString(Context context, long daysSinceEpoch) {
        LocalDate date = Instant.ofEpochSecond(daysSinceEpoch * 24 * 60 * 60)
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
        return localDateToDateString(context, date);
    }

    /**
     * @param context   Context to extract date format
     * @param localDate Local date
     * @return Date string in local format
     */
    public static String localDateToDateString(Context context, LocalDate localDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                .withLocale(context.getResources().getConfiguration().getLocales().get(0));
        return localDate.format(formatter);
    }

    /**
     * @param context        Context to extract date format
     * @param dateTimeString String containing date and time
     * @return Seconds since epoch of date/time
     */
    public static long dateTimeStringToSecondsSinceEpoch(Context context, String dateTimeString) {
        String[] dateTimeComponents = dateTimeString.split(" ", 2);
        if (dateTimeComponents.length != 2) {
            return -1;
        }

        LocalDate date = dateStringToDate(context, dateTimeComponents[0]);
        if (date == null) {
            return -1;
        }

        int minutes = timeStringToMinutes(context, dateTimeComponents[1]);
        if (minutes == -1) {
            return -1;
        }

        return changeTimeStampDate(instantFromTodayMinutes(minutes).getEpochSecond(), date);
    }

    /**
     * @param dateString Date string in local format
     * @return Local date
     */
    public static @Nullable LocalDate dateStringToDate(Context context, String dateString) {
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
        } catch (DateTimeParseException e) {
            try {
                Date date = DateFormat.getDateFormat(context).parse(dateString);
                if (date != null) {
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
            } catch (ParseException ignored) {
                // Intentionally empty
            }

        }
        return null;
    }


    /**
     * @param context    Context to extract time format
     * @param timeString Time string in local format
     * @return Minutes since midnight
     */
    public static int timeStringToMinutes(Context context, String timeString) {
        try {
            Date date = DateFormat.getTimeFormat(context).parse(timeString);
            return date != null ? date.toInstant().atOffset(EPOCH_OFFSET).toLocalTime().toSecondOfDay() / 60 : -1;
        } catch (ParseException e) {
            return -1;
        }
    }

    /**
     * @param remindedTimestamp Time stamp in seconds since epoch
     * @param localDate         Local date
     * @return Time stamp in seconds since epoch with given date
     */
    public static long changeTimeStampDate(long remindedTimestamp, LocalDate localDate) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(remindedTimestamp), ZoneId.systemDefault());
        localDateTime = localDate != null ? localDateTime.with(localDate) : localDateTime;
        return localDateTime.toEpochSecond(ZoneId.systemDefault().getRules().getOffset(localDateTime));
    }

    /**
     * @param minutes Minutes since midnight
     * @return Instant of today at given time
     */
    public static Instant instantFromTodayMinutes(int minutes) {
        LocalDate date = LocalDate.now();
        LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.of((minutes / 60), (minutes % 60)));
        return dateTime.toInstant(ZoneId.systemDefault().getRules().getOffset(dateTime));
    }

    /**
     * @param timeStamp    Time stamp in seconds since epoch
     * @param localMinutes Minutes since midnight
     * @return Time stamp in seconds since epoch with given minutes
     */
    public static long changeTimeStampMinutes(long timeStamp, int localMinutes) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeStamp), ZoneId.systemDefault());
        localDateTime = localDateTime.withHour(localMinutes / 60).withMinute(localMinutes % 60);
        return localDateTime.toEpochSecond(ZoneId.systemDefault().getRules().getOffset(localDateTime));
    }

    /**
     * @param context   Context to extract date and time formats
     * @param timeStamp Time stamp in seconds since epoch
     * @return Date and time string in local format as relative date time string
     */
    public static String toConfigurableDateTimeString(Context context, SharedPreferences preferences, long timeStamp) {
        if (preferences.getBoolean(USE_RELATIVE_DATE_TIME, false)) {
            return DateUtils.getRelativeDateTimeString(context, timeStamp * 1000, DateUtils.MINUTE_IN_MILLIS, DateUtils.DAY_IN_MILLIS * 2, DateUtils.FORMAT_SHOW_TIME).toString();
        } else {
            return toLocalizedDatetimeString(context, timeStamp);
        }
    }

    /**
     * @param context   Context to extract date and time formats
     * @param timeStamp Time stamp in seconds since epoch
     * @return Date and time string in local format
     */
    public static String toLocalizedDatetimeString(Context context, long timeStamp) {
        return toLocalizedDateString(context, timeStamp) + " " + toLocalizedTimeString(context, timeStamp);
    }

    /**
     * @param context   Context to extract date format
     * @param timeStamp Time stamp in seconds since epoch
     * @return Date string in local format
     */
    public static String toLocalizedDateString(Context context, long timeStamp) {
        return DateFormat.getDateFormat(context).format(Date.from(Instant.ofEpochSecond(timeStamp)));
    }

    /**
     * @param context   Context to extract time format
     * @param timeStamp Time stamp in seconds since epoch
     * @return Time string in local format
     */
    public static String toLocalizedTimeString(Context context, long timeStamp) {
        return DateFormat.getTimeFormat(context).format(Date.from(Instant.ofEpochSecond(timeStamp)));
    }

    public static Object toISO8601DatetimeString(long remindedTimestamp) {
        return Instant.ofEpochSecond(remindedTimestamp).toString();
    }

    public interface TimePickerResult {
        void onTimeSelected(int minutes);
    }

    public interface DatePickerResult {
        void onDateSelected(long daysSinceEpoch);
    }

    public static class TimePickerWrapper {
        final FragmentActivity activity;
        private final Integer titleText;
        private final Integer timeFormat;

        public TimePickerWrapper(FragmentActivity activity) {
            this.activity = activity;
            this.titleText = null;
            this.timeFormat = DateFormat.is24HourFormat(activity) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;
        }

        public TimePickerWrapper(FragmentActivity activity, @StringRes int titleText, int timeFormat) {
            this.activity = activity;
            this.titleText = titleText;
            this.timeFormat = timeFormat;
        }

        public void show(int hourOfDay, int minute, TimePickerResult timePickerResult) {
            MaterialTimePicker.Builder builder = new MaterialTimePicker.Builder()
                    .setTimeFormat(timeFormat)
                    .setHour(hourOfDay)
                    .setMinute(minute)
                    .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK);

            if (titleText != null) {
                builder.setTitleText(titleText);
            }

            MaterialTimePicker timePickerDialog = builder.build();
            timePickerDialog.addOnPositiveButtonClickListener(view -> timePickerResult.onTimeSelected(timePickerDialog.getHour() * 60 + timePickerDialog.getMinute()));

            timePickerDialog.show(activity.getSupportFragmentManager(), "time_picker");
        }
    }

    public static class DatePickerWrapper {
        final FragmentManager fragmentManager;
        private final Integer titleText;

        public DatePickerWrapper(FragmentManager fragmentManager) {
            this.fragmentManager = fragmentManager;
            this.titleText = null;
        }

        public DatePickerWrapper(FragmentManager fragmentManager, @StringRes int titleText) {
            this.fragmentManager = fragmentManager;
            this.titleText = titleText;
        }

        public void show(LocalDate startDate, DatePickerResult datePickerResult) {
            if (startDate == null) {
                startDate = LocalDate.now();
            }
            MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                    .setSelection(startDate.toEpochDay() * DateUtils.DAY_IN_MILLIS);

            if (titleText != null) {
                builder.setTitleText(titleText);
            }

            MaterialDatePicker<Long> datePickerDialog = builder.build();
            datePickerDialog.addOnPositiveButtonClickListener(selectedDate -> datePickerResult.onDateSelected(selectedDate / DateUtils.DAY_IN_MILLIS));

            datePickerDialog.show(fragmentManager, "date_picker");
        }
    }
}
