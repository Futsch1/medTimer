package com.futsch1.medtimer.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.futsch1.medtimer.model.ReminderTime
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeFormatter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesDataSource: PreferencesDataSource,
    private val localeContextAccessor: LocaleContextAccessor
) {
    private val epochOffset: ZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.ofEpochSecond(0))

    /**
     * @param minutes Minutes since midnight
     * @return Time string in local format
     */
    fun minutesToTimeString(minutes: Int): String {
        try {
            val calendar = Calendar.getInstance()
            calendar.clear()
            calendar.add(Calendar.MINUTE, minutes)
            return DateFormat.getTimeFormat(localeContextAccessor.getLocaleAwareContext()).format(calendar.time)
        } catch (_: DateTimeException) {
            return minutesToDurationString(minutes)
        }
    }

    fun toTimeString(localTime: LocalTime): String {
        val dateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(getLocale())
        return localTime.format(dateTimeFormatter)
    }

    fun toTimeString(reminderTime: ReminderTime): String {
        return if (reminderTime.isDuration) {
            minutesToDurationString(reminderTime.minutes)
        } else {
            toTimeString(reminderTime.getLocalTime())
        }
    }

    /**
     * @param minutes Minutes since midnight
     * @return Time string in local format
     */
    @SuppressLint("DefaultLocale")
    fun minutesToDurationString(minutes: Int): String {
        return String.format("%d:%02d", minutes / 60, minutes % 60)
    }

    /**
     * @param date Local date
     * @return Date string in local format
     */
    fun localDateToString(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(getLocale())
        return date.format(formatter)
    }

    /**
     * @param days Days since epoch
     * @return Date string in local format
     */
    fun daysSinceEpochToDateString(days: Long): String {
        return localDateToString(LocalDate.ofEpochDay(days))
    }

    fun toTimeString(instant: Instant): String {
        return DateFormat.getTimeFormat(localeContextAccessor.getLocaleAwareContext()).format(Date.from(instant))
    }

    fun toDateString(instant: Instant): String {
        return DateFormat.getDateFormat(localeContextAccessor.getLocaleAwareContext()).format(Date.from(instant))
    }

    fun toDateTimeString(instant: Instant): String {
        return "${toDateString(instant)} ${toTimeString(instant)}"
    }

    fun toConfigurableDateTimeString(instant: Instant): String {
        return if (preferencesDataSource.preferences.value.useRelativeDateTime) {
            DateUtils.getRelativeDateTimeString(
                localeContextAccessor.getLocaleAwareContext(),
                instant.toEpochMilli(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.DAY_IN_MILLIS * 2,
                DateUtils.FORMAT_SHOW_TIME
            ).toString()
        } else {
            toDateTimeString(instant)
        }
    }

    fun toConfigurableTimeString(instant: Instant, isShort: Boolean): String {
        return if (preferencesDataSource.preferences.value.useRelativeDateTime) {
            if (isShort) {
                DateUtils.getRelativeTimeSpanString(instant.toEpochMilli(), Instant.now().toEpochMilli(), DateUtils.MINUTE_IN_MILLIS).toString()
            } else {
                DateUtils.getRelativeDateTimeString(
                    localeContextAccessor.getLocaleAwareContext(),
                    instant.toEpochMilli(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.DAY_IN_MILLIS * 2,
                    DateUtils.FORMAT_SHOW_TIME
                ).toString()
            }
        } else {
            toTimeString(instant)
        }
    }

    /**
     * Converts a local date time to a date time string
     *
     * @param localDateTime The local date time.
     * @return A string representing the date and time in the localized format.
     */
    fun toDateTimeString(localDateTime: LocalDateTime): String {
        val instant = localDateTime.toInstant(ZoneId.systemDefault().rules.getOffset(localDateTime))
        return toDateTimeString(instant)
    }

    /**
     * @param dateString Date string in local format
     * @return Local date
     */
    fun stringToLocalDate(dateString: String): LocalDate? {
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
        } catch (_: DateTimeParseException) {
            try {
                val date = DateFormat.getDateFormat(localeContextAccessor.getLocaleAwareContext()).parse(dateString)
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
     * @param timeString Time string in local format
     * @return Minutes since midnight
     */
    fun timeStringToMinutes(timeString: String): Int {
        try {
            val date = DateFormat.getTimeFormat(localeContextAccessor.getLocaleAwareContext()).parse(timeString)
            return if (date != null) date.toInstant().atOffset(epochOffset).toLocalTime().toSecondOfDay() / 60 else -1
        } catch (_: ParseException) {
            return -1
        }
    }

    /**
     * @param dateTimeString String containing date and time
     * @return Seconds since epoch of date/time
     */
    fun stringToInstant(dateTimeString: String): Instant? {
        val dateTimeComponents = dateTimeString.split(" ".toRegex(), limit = 2).toTypedArray()
        if (dateTimeComponents.size != 2) {
            return null
        }

        val date = stringToLocalDate(dateTimeComponents[0]) ?: return null
        val minutes = timeStringToMinutes(dateTimeComponents[1])
        if (minutes == -1) {
            return null
        }

        return TimeHelper.changeInstantDate(TimeHelper.instantFromDateAndMinutes(minutes, LocalDate.now()), date)
    }

    private fun getLocale(): Locale {
        val localeList = context.resources.configuration.getLocales()
        return if (preferencesDataSource.preferences.value.systemLocale && localeList.size() > 1) {
            localeList[1]
        } else {
            localeList[0]
        }
    }
}