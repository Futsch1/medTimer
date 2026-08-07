package com.futsch1.medtimer.core.ui.time

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.futsch1.medtimer.core.common.helpers.withPrimaryLocale
import java.text.ParseException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * The system's date/time format, shared by TimeFormatter and the Compose remember* hooks so that a
 * change to the rules reaches both. Formatting and parsing live together because they have to keep
 * agreeing on the same pattern.
 */
object SystemDateTimeFormat {
    fun time(context: Context, date: Date): String =
        DateFormat.getTimeFormat(context.withPrimaryLocale()).format(date)

    fun time(context: Context, instant: Instant): String = time(context, Date.from(instant))

    fun date(context: Context, date: Date): String =
        DateFormat.getDateFormat(context.withPrimaryLocale()).format(date)

    fun date(context: Context, instant: Instant): String = date(context, Date.from(instant))

    fun date(context: Context, date: LocalDate): String =
        date(context, Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()))

    fun dateTime(context: Context, instant: Instant): String =
        "${date(context, instant)} ${time(context, instant)}"

    fun relativeDateTime(context: Context, instant: Instant): String =
        DateUtils.getRelativeDateTimeString(
            context.withPrimaryLocale(),
            instant.toEpochMilli(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.DAY_IN_MILLIS * 2,
            DateUtils.FORMAT_SHOW_TIME,
        ).toString()

    fun relativeTimeSpan(instant: Instant): String =
        DateUtils.getRelativeTimeSpanString(
            instant.toEpochMilli(),
            Instant.now().toEpochMilli(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()

    fun parseDate(context: Context, text: String): Date? = try {
        DateFormat.getDateFormat(context.withPrimaryLocale()).parse(text)
    } catch (_: ParseException) {
        null
    }

    fun parseTime(context: Context, text: String): Date? = try {
        DateFormat.getTimeFormat(context.withPrimaryLocale()).parse(text)
    } catch (_: ParseException) {
        null
    }
}
