package com.futsch1.medtimer.helpers

import android.content.Context
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

// TODO: a thin wrapper over TimeHelper; inline TimeHelper implementation into it after all its usages are migrated to the TimeFormatter
@Singleton
class TimeFormatter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesDataSource: PreferencesDataSource
) {
    fun minutesToTimeString(minutes: Long): String =
        TimeHelper.minutesToTimeString(context, minutes)

    fun minutesToTimeString(minutes: Int): String =
        minutesToTimeString(minutes.toLong())

    fun localDateToString(date: LocalDate): String =
        TimeHelper.localDateToString(context, date, preferencesDataSource)

    fun daysSinceEpochToDateString(days: Long): String =
        TimeHelper.daysSinceEpochToDateString(context, days, preferencesDataSource)

    fun secondsSinceEpochToTimeString(timestamp: Long): String =
        TimeHelper.secondsSinceEpochToTimeString(context, timestamp)

    fun secondSinceEpochToDateString(timestamp: Long): String =
        TimeHelper.secondSinceEpochToDateString(context, timestamp)

    fun secondsSinceEpochToDateTimeString(timestamp: Long): String =
        TimeHelper.secondsSinceEpochToDateTimeString(context, timestamp)

    fun secondsSinceEpochToConfigurableDateTimeString(timestamp: Long): String =
        TimeHelper.secondsSinceEpochToConfigurableDateTimeString(context, preferencesDataSource, timestamp)

    fun secondsSinceEpochToConfigurableTimeString(timestamp: Long, isShort: Boolean): String =
        TimeHelper.secondsSinceEpochToConfigurableTimeString(context, preferencesDataSource, timestamp, isShort)

    fun localeDateTimeToDateTimeString(localDateTime: LocalDateTime): String =
        TimeHelper.localeDateTimeToDateTimeString(context, localDateTime)

    fun stringToLocalDate(dateString: String): LocalDate? =
        TimeHelper.stringToLocalDate(context, dateString)

    fun timeStringToMinutes(timeString: String): Int =
        TimeHelper.timeStringToMinutes(context, timeString)

    fun stringToSecondsSinceEpoch(dateTimeString: String): Long =
        TimeHelper.stringToSecondsSinceEpoch(context, dateTimeString)

    fun minutesToDurationString(minutes: Long): String =
        TimeHelper.minutesToDurationString(minutes)
}
