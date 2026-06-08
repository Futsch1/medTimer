package com.futsch1.medtimer.core.common.helpers

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
        return LocalDate.ofInstant(Instant.ofEpochSecond(secondsSinceEpoch), zoneId)
    }

    /**
     * @param secondsSinceEpoch Seconds since epoch
     * @param zoneId            Zone id
     * @return Local date/time
     */
    fun secondsSinceEpochToLocalTime(secondsSinceEpoch: Long, zoneId: ZoneId): LocalTime {
        return LocalTime.ofInstant(Instant.ofEpochSecond(secondsSinceEpoch), zoneId)
    }

    fun changeInstantDate(instant: Instant, localDate: LocalDate?): Instant {
        var localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        localDateTime = if (localDate != null) localDateTime.with(localDate) else localDateTime
        return localDateTime.toInstant(ZoneId.systemDefault().rules.getOffset(localDateTime))
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
     * @param time    Time instant
     * @param localMinutes Minutes since midnight
     * @return Time stamp in seconds since epoch with given minutes
     */
    fun changeTimeMinutes(time: Instant, localMinutes: Int): Instant {
        val zonedDateTime = time.atZone(ZoneId.systemDefault())
        val localDateTime = zonedDateTime.withHour(localMinutes / 60).withMinute(localMinutes % 60)
        return localDateTime.toInstant()
    }

    fun secondsSinceEpochToISO8601DatetimeString(remindedTimestamp: Long): Any {
        return Instant.ofEpochSecond(remindedTimestamp).toString()
    }

    fun isSameDay(instantOne: Instant, instantTwo: Instant): Boolean {
        return instantOne.atZone(ZoneId.systemDefault()).toLocalDate() == instantTwo.atZone(
            ZoneId.systemDefault()
        ).toLocalDate()
    }

    fun isOnDay(epochSeconds: Long, epochDay: Long, systemZone: ZoneId): Boolean {
        val dayInterval = zoneIdCaches.getOrPut(systemZone) { Cache(systemZone) }.getDaySecondsStartEnd(epochDay)
        return dayInterval.first <= epochSeconds && epochSeconds < dayInterval.second
    }

    val zoneIdCaches: MutableMap<ZoneId, Cache> = mutableMapOf()

    class Cache(val zoneId: ZoneId) {
        val daySecondsStartEndCache: MutableMap<Long, Pair<Long, Long>> = mutableMapOf()

        fun getDaySecondsStartEnd(epochDay: Long): Pair<Long, Long> {
            return daySecondsStartEndCache.getOrPut(epochDay) {
                val start = LocalDate.ofEpochDay(epochDay).atStartOfDay(zoneId).toInstant()
                val end = LocalDate.ofEpochDay(epochDay + 1).atStartOfDay(zoneId).toInstant()
                Pair(start.epochSecond, end.epochSecond)
            }
        }
    }
}

