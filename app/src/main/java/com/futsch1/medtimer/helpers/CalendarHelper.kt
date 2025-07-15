package com.futsch1.medtimer.helpers

import android.content.Intent
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId

fun createCalendarEventIntent(title: String, date: LocalDate): Intent {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        val datetime = date.atStartOfDay()
        val datetimeInEpochMillis = datetime.toInstant(ZoneId.systemDefault().rules.getOffset(datetime)).toEpochMilli()
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, datetimeInEpochMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, datetimeInEpochMillis + 1000 * 60 * 60 * 24)
        putExtra(CalendarContract.Events.ALL_DAY, true)
    }
    return intent
}