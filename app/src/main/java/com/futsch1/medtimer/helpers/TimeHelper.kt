package com.futsch1.medtimer.helpers

import android.annotation.SuppressLint
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField

object TimeHelper {
    /**
     * @param minutes Minutes since midnight
     * @return Time string in local format
     */
    @SuppressLint("DefaultLocale")
    fun minutesToDurationString(minutes: Int): String {
        return String.format("%d:%02d", minutes / 60, minutes % 60)
    }

    /**
     * @param timeString Time string in local format
     * @return Minutes since midnight
     */
    fun durationStringToMinutes(timeString: String): Int {
        try {
            val accessor = DateTimeFormatter.ofPattern("H:mm").parse(timeString)
            return accessor[ChronoField.HOUR_OF_DAY] * 60 + accessor[ChronoField.MINUTE_OF_HOUR]
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

    fun secondsSinceEpochToISO8601DatetimeString(remindedTimestamp: Long): Any {
        return Instant.ofEpochSecond(remindedTimestamp).toString()
    }

    fun isSameDay(secondsSinceEpochOne: Long, secondsSinceEpochTwo: Long): Boolean {
        return Instant.ofEpochSecond(secondsSinceEpochOne).atZone(ZoneId.systemDefault()).toLocalDate() == Instant.ofEpochSecond(secondsSinceEpochTwo).atZone(
            ZoneId.systemDefault()
        ).toLocalDate()
    }

}

