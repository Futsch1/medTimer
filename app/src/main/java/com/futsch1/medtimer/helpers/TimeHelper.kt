package com.futsch1.medtimer.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.preferences.PreferencesNames
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.ParseException
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeHelper {
    private val EPOCH_OFFSET: ZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.ofEpochSecond(0))

    /**
     * @param context Context to extract time format
     * @param minutes Minutes since midnight
     * @return Time string in local format
     */
    @JvmStatic
    fun minutesToTimeString(context: Context, minutes: Long): String {
        try {
            val time = minutesToDate(minutes.toInt())
            return DateFormat.getTimeFormat(LocaleContextWrapper(context)).format(time)
        } catch (_: DateTimeException) {
            return minutesToDurationString(minutes)
        }
    }

    /**
     * @param minutes Minutes since midnight
     * @return Date of local time on epoch day 0
     */
    fun minutesToDate(minutes: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.add(Calendar.MINUTE, minutes)
        return calendar.getTime()
    }

    /**
     * @param minutes Minutes since midnight
     * @return Time string in local format
     */
    @JvmStatic
    @SuppressLint("DefaultLocale")
    fun minutesToDurationString(minutes: Long): String {
        return String.format("%d:%02d", minutes / 60, minutes % 60)
    }

    /**
     * @param timeString Time string in local format
     * @return Minutes since midnight
     */
    fun durationStringToMinutes(timeString: String): Int {
        try {
            val accessor = DateTimeFormatter.ofPattern("H:mm").parse(timeString)
            return accessor.get(ChronoField.HOUR_OF_DAY) * 60 + accessor.get(ChronoField.MINUTE_OF_HOUR)
        } catch (_: DateTimeParseException) {
            return -1
        }
    }

    /**
     * @param secondsSinceEpoch Seconds since epoch
     * @param zoneId            Zone id
     * @return Local date
     */
    fun secondsSinceEpochToLocalDate(secondsSinceEpoch: Long, zoneId: ZoneId): LocalDate {
        return Instant.ofEpochSecond(secondsSinceEpoch).atZone(zoneId).toLocalDate()
    }

    /**
     * @param secondsSinceEpoch Seconds since epoch
     * @param zoneId            Zone id
     * @return Local date/time
     */
    fun secondsSinceEpochToLocalTime(secondsSinceEpoch: Long, zoneId: ZoneId): LocalTime {
        return Instant.ofEpochSecond(secondsSinceEpoch).atZone(zoneId).toLocalTime()
    }

    /**
     * @param context        Context to extract date format
     * @param daysSinceEpoch Days since epoch
     * @return Date string in local format
     */
    @JvmStatic
    fun daysSinceEpochToDateString(context: Context, daysSinceEpoch: Long): String {
        val date = LocalDate.ofEpochDay(daysSinceEpoch)
        return localDateToString(context, date)
    }

    /**
     * @param context   Context to extract date format
     * @param localDate Local date
     * @return Date string in local format
     */
    fun localDateToString(context: Context, localDate: LocalDate): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(getLocale(context))
        return localDate.format(formatter)
    }

    private fun getLocale(context: Context): Locale {
        val localeList = context.resources.configuration.getLocales()
        val locale = if (useSystemLocale(context) && localeList.size() > 1) {
            localeList.get(1)
        } else {
            localeList.get(0)
        }
        return locale
    }

    private fun useSystemLocale(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferencesNames.SYSTEM_LOCALE, false)
    }

    /**
     * @param context        Context to extract date format
     * @param dateTimeString String containing date and time
     * @return Seconds since epoch of date/time
     */
    fun stringToSecondsSinceEpoch(context: Context, dateTimeString: String): Long {
        val dateTimeComponents: Array<String?> = dateTimeString.split(" ".toRegex(), limit = 2).toTypedArray()
        if (dateTimeComponents.size != 2) {
            return -1
        }

        val date = stringToLocalDate(context, dateTimeComponents[0]!!) ?: return -1

        val minutes = timeStringToMinutes(context, dateTimeComponents[1]!!)
        if (minutes == -1) {
            return -1
        }

        return changeTimeStampDate(instantFromDateAndMinutes(minutes, LocalDate.now()).epochSecond, date)
    }

    /**
     * @param dateString Date string in local format
     * @return Local date
     */
    fun stringToLocalDate(context: Context, dateString: String): LocalDate? {
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
        } catch (_: DateTimeParseException) {
            try {
                val date = DateFormat.getDateFormat(LocaleContextWrapper(context)).parse(dateString)
                if (date != null) {
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                }
            } catch (_: ParseException) {
                // Intentionally empty
            }
        }
        return null
    }

    /**
     * @param context    Context to extract time format
     * @param timeString Time string in local format
     * @return Minutes since midnight
     */
    fun timeStringToMinutes(context: Context, timeString: String): Int {
        try {
            val date = DateFormat.getTimeFormat(LocaleContextWrapper(context)).parse(timeString)
            return if (date != null) date.toInstant().atOffset(EPOCH_OFFSET).toLocalTime().toSecondOfDay() / 60 else -1
        } catch (_: ParseException) {
            return -1
        }
    }

    /**
     * @param remindedTimestamp Time stamp in seconds since epoch
     * @param localDate         Local date
     * @return Time stamp in seconds since epoch with given date
     */
    fun changeTimeStampDate(remindedTimestamp: Long, localDate: LocalDate?): Long {
        var localDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(remindedTimestamp), ZoneId.systemDefault())
        localDateTime = if (localDate != null) localDateTime.with(localDate) else localDateTime
        return localDateTime.toEpochSecond(ZoneId.systemDefault().rules.getOffset(localDateTime))
    }

    /**
     * @param minutes Minutes since midnight
     * @return Instant of today at given time
     */
    fun instantFromDateAndMinutes(minutes: Int, date: LocalDate): Instant {
        val dateTime = LocalDateTime.of(date, LocalTime.of((minutes / 60), (minutes % 60)))
        return dateTime.toInstant(ZoneId.systemDefault().rules.getOffset(dateTime))
    }

    /**
     * @param timeStamp    Time stamp in seconds since epoch
     * @param localMinutes Minutes since midnight
     * @return Time stamp in seconds since epoch with given minutes
     */
    fun changeTimeStampMinutes(timeStamp: Long, localMinutes: Int): Long {
        var localDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeStamp), ZoneId.systemDefault())
        localDateTime = localDateTime.withHour(localMinutes / 60).withMinute(localMinutes % 60)
        return localDateTime.toEpochSecond(ZoneId.systemDefault().rules.getOffset(localDateTime))
    }

    /**
     * @param context   Context to extract date and time formats
     * @param timeStamp Time stamp in seconds since epoch
     * @return Date and time string in local format as relative date time string
     */
    fun secondsSinceEpochToConfigurableDateTimeString(context: Context, preferences: SharedPreferences, timeStamp: Long): String {
        return if (preferences.getBoolean(PreferencesNames.USE_RELATIVE_DATE_TIME, false)) {
            DateUtils.getRelativeDateTimeString(
                LocaleContextWrapper(context),
                timeStamp * 1000,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.DAY_IN_MILLIS * 2,
                DateUtils.FORMAT_SHOW_TIME
            ).toString()
        } else {
            secondsSinceEpochToDateTimeString(context, timeStamp)
        }
    }

    /**
     * @param context   Context to extract date and time formats
     * @param timeStamp Time stamp in seconds since epoch
     * @return Date and time string in local format
     */
    @JvmStatic
    fun secondsSinceEpochToDateTimeString(context: Context, timeStamp: Long): String {
        return secondSinceEpochToDateString(context, timeStamp) + " " + secondsSinceEpochToTimeString(context, timeStamp)
    }

    /**
     * @param context   Context to extract date format
     * @param timeStamp Time stamp in seconds since epoch
     * @return Date string in local format
     */
    fun secondSinceEpochToDateString(context: Context, timeStamp: Long): String {
        return secondSinceEpochToDateString(DateFormat.getDateFormat(LocaleContextWrapper(context)), timeStamp)
    }

    /**
     * @param context   Context to extract time format
     * @param timeStamp Time stamp in seconds since epoch
     * @return Time string in local format
     */
    fun secondsSinceEpochToTimeString(context: Context, timeStamp: Long): String {
        return secondsSinceEpochToTimeString(DateFormat.getTimeFormat(LocaleContextWrapper(context)), timeStamp)
    }

    /**
     * @param format    Date format to use
     * @param timeStamp Time stamp in seconds since epoch
     * @return Date string in local format
     */
    private fun secondSinceEpochToDateString(format: java.text.DateFormat, timeStamp: Long): String {
        return format.format(Date.from(Instant.ofEpochSecond(timeStamp)))
    }

    /**
     * @param format    Date format to use
     * @param timeStamp Time stamp in seconds since epoch
     * @return Time string in local format
     */
    private fun secondsSinceEpochToTimeString(format: java.text.DateFormat, timeStamp: Long): String {
        return format.format(Date.from(Instant.ofEpochSecond(timeStamp)))
    }

    /**
     * @param dateFormat DateFormat
     * @param timeFormat DateFormat
     * @param timeStamp  Time stamp in seconds since epoch
     * @return Date and time string in local format
     */
    private fun secondsSinceEpochToDateTimeString(dateFormat: java.text.DateFormat, timeFormat: java.text.DateFormat, timeStamp: Long): String {
        return secondSinceEpochToDateString(dateFormat, timeStamp) + " " + secondsSinceEpochToTimeString(timeFormat, timeStamp)
    }

    /**
     * @param context     Context to extract date and time formats
     * @param preferences SharedPreferences to use for checking if relative date time is active
     * @param timeStamp   Time stamp in seconds since epoch
     * @param isShort     Whether to show the actual time stamp or not
     * @return Date and time string in local format as relative date time string
     */
    fun secondsSinceEpochToConfigurableTimeString(context: Context, preferences: SharedPreferences, timeStamp: Long, isShort: Boolean): String {
        return if (preferences.getBoolean(PreferencesNames.USE_RELATIVE_DATE_TIME, false)) {
            if (isShort) {
                DateUtils.getRelativeTimeSpanString(timeStamp * 1000, Instant.now().toEpochMilli(), DateUtils.MINUTE_IN_MILLIS).toString()
            } else {
                DateUtils.getRelativeDateTimeString(
                    LocaleContextWrapper(context),
                    timeStamp * 1000,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.DAY_IN_MILLIS * 2,
                    DateUtils.FORMAT_SHOW_TIME
                ).toString()
            }
        } else {
            secondsSinceEpochToTimeString(context, timeStamp)
        }
    }

    /**
     * Converts a local date time to a date time string
     *
     * @param context       Context to access locale and date/time format settings.
     * @param localDateTime The timestamp in seconds since the epoch.
     * @return A string representing the date and time in the localized format, e.g., "Jan 1, 2023 10:00 AM".
     */
    fun localeDateTimeToDateTimeString(context: Context, localDateTime: LocalDateTime): String {
        val epochSecond = localDateTime.toEpochSecond(ZoneId.systemDefault().rules.getOffset(localDateTime))
        return secondsSinceEpochToDateTimeString(context, epochSecond)
    }

    @JvmStatic
    fun secondsSinceEpochToISO8601DatetimeString(remindedTimestamp: Long): Any {
        return Instant.ofEpochSecond(remindedTimestamp).toString()
    }

    fun onChangedUseSystemLocale() {
        LocaleContextWrapper.resetLocaleContextWrapper()
    }

    fun isSameDay(secondsSinceEpochOne: Long, secondsSinceEpochTwo: Long): Boolean {
        return Instant.ofEpochSecond(secondsSinceEpochOne).atZone(ZoneId.systemDefault()).toLocalDate() == Instant.ofEpochSecond(secondsSinceEpochTwo).atZone(
            ZoneId.systemDefault()
        ).toLocalDate()
    }

    private class LocaleContextWrapper(base: Context) : ContextWrapper(base) {
        init {
            buildContext(base)
        }

        override fun getResources(): Resources {
            return mLocaleAwareContext!!.resources
        }

        companion object {
            @SuppressLint("StaticFieldLeak") // No leak because this context is just a single copy of the enclosing context
            private var mLocaleAwareContext: Context? = null

            @SuppressLint("AppBundleLocaleChanges") // This is ok because the locale is not changed for the complete context, only wrapped for the DateFormat calls
            @Synchronized
            private fun buildContext(base: Context) {
                if (mLocaleAwareContext != null) {
                    return
                }

                if (base.resources != null && base.resources.configuration != null) {
                    val configuration = Configuration(base.resources.configuration)
                    configuration.setLocales(LocaleList(getLocale(base)))
                    mLocaleAwareContext = base.createConfigurationContext(configuration)
                } else {
                    mLocaleAwareContext = base
                }
            }

            @Synchronized
            fun resetLocaleContextWrapper() {
                mLocaleAwareContext = null
            }
        }
    }

    class TimePickerWrapper {
        val activity: FragmentActivity
        private val titleText: Int?
        private val timeFormat: Int

        constructor(activity: FragmentActivity) {
            this.activity = activity
            this.titleText = null
            this.timeFormat = if (DateFormat.is24HourFormat(LocaleContextWrapper(activity))) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        }

        constructor(activity: FragmentActivity, @StringRes titleText: Int, timeFormat: Int) {
            this.activity = activity
            this.titleText = titleText
            this.timeFormat = timeFormat
        }

        fun show(hourOfDay: Int, minute: Int, timePickerResult: (minutes: Int) -> Unit) {
            val builder = MaterialTimePicker.Builder()
                .setTimeFormat(timeFormat)
                .setHour(hourOfDay)
                .setMinute(minute)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)

            if (titleText != null) {
                builder.setTitleText(titleText)
            }

            val timePickerDialog = builder.build()
            timePickerDialog.addOnPositiveButtonClickListener { _: View? -> timePickerResult(timePickerDialog.hour * 60 + timePickerDialog.minute) }

            timePickerDialog.show(activity.supportFragmentManager, "time_picker")
        }
    }

    @JvmRecord
    data class DatePickerWrapper(val activity: FragmentActivity) {
        fun show(startDate: LocalDate?, datePickerResult: (daysSinceEpoch: Long) -> Unit) {
            var startDate = startDate
            if (startDate == null) {
                startDate = LocalDate.now()
            }
            val builder = MaterialDatePicker.Builder.datePicker()
                .setSelection(startDate.toEpochDay() * DateUtils.DAY_IN_MILLIS)

            val datePickerDialog = builder.build()
            datePickerDialog.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { selectedDate: Long? ->
                datePickerResult(
                    selectedDate!! / DateUtils.DAY_IN_MILLIS
                )
            })

            datePickerDialog.show(activity.supportFragmentManager, "date_picker")
        }
    }

    class QuickSecondsSinceEpochFormatter(context: Context) {
        var cachedDateFormat: java.text.DateFormat
        var cachedTimeFormat: java.text.DateFormat

        init {
            val wrapper = LocaleContextWrapper(context)
            cachedDateFormat = DateFormat.getDateFormat(wrapper)
            cachedTimeFormat = DateFormat.getTimeFormat(wrapper)
        }

        fun secondsSinceEpochToDateTimeString(timeStamp: Long): String {
            return secondsSinceEpochToDateTimeString(cachedDateFormat, cachedTimeFormat, timeStamp)
        }
    }
}
