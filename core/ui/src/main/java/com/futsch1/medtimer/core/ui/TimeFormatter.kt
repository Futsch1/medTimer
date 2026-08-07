package com.futsch1.medtimer.core.ui

import android.annotation.SuppressLint
import android.content.Context
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderTime
import com.futsch1.medtimer.core.ui.time.SystemDateTimeFormat
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeFormatter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesDataSource: PreferencesDataSource,
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
            return SystemDateTimeFormat.time(context, calendar.time)
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

    fun toTimeString(instant: Instant): String = SystemDateTimeFormat.time(context, instant)

    fun toDateString(instant: Instant): String = SystemDateTimeFormat.date(context, instant)

    fun toDateTimeString(instant: Instant): String = SystemDateTimeFormat.dateTime(context, instant)

    fun toConfigurableDateTimeString(instant: Instant): String {
        return if (preferencesDataSource.preferences.value.useRelativeDateTime) {
            SystemDateTimeFormat.relativeDateTime(context, instant)
        } else {
            toDateTimeString(instant)
        }
    }

    fun toConfigurableTimeString(instant: Instant, isShort: Boolean): String {
        return if (preferencesDataSource.preferences.value.useRelativeDateTime) {
            if (isShort) {
                SystemDateTimeFormat.relativeTimeSpan(instant)
            } else {
                SystemDateTimeFormat.relativeDateTime(context, instant)
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
            return LocalDate.parse(dateString, DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(getLocale()))
        } catch (_: DateTimeParseException) {
            val date = SystemDateTimeFormat.parseDate(context, dateString)
            if (date != null) {
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }
        return null
    }

    /**
     * @param timeString Time string in local format
     * @return Minutes since midnight
     */
    fun timeStringToMinutes(timeString: String): Int {
        val date = SystemDateTimeFormat.parseTime(context, timeString) ?: return -1
        return date.toInstant().atOffset(epochOffset).toLocalTime().toSecondOfDay() / 60
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
        val localeList = context.resources.configuration.locales
        return if (preferencesDataSource.preferences.value.systemLocale && localeList.size() > 1) {
            localeList[1]
        } else {
            localeList[0]
        }
    }
}
