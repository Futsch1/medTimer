package com.futsch1.medtimer.feature.ui.overview.actions

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.core.common.helpers.DatePickerDialogFactory
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.TimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Asks for the time a dose was really taken. Only a time picker opens, on the date of the initial
 * instant, which its title shows. Its Change date button opens a date picker on the date in effect.
 * Date OK makes the picked day the date in effect, date Cancel keeps it; both return to the time
 * picker with the picked time kept. Only time picker OK marks. A time in the future is rejected, and
 * the time picker opens again on the current time with a hint, because a future taken time would
 * shift the next due time of interval and linked reminders.
 */
class TakenTimePicker @Inject constructor(
    private val datePickerDialogFactory: DatePickerDialogFactory,
    private val timePickerDialogFactory: TimePickerDialogFactory,
    private val fragmentActivity: FragmentActivity,
    private val timeAccess: TimeAccess,
    private val timeFormatter: TimeFormatter,
) {
    /** Opens the time picker on [initial] and hands the confirmed time to [onPicked]. Cancel calls nothing. */
    fun pick(initial: Instant, onPicked: (Instant) -> Unit) {
        val initialDateTime = initial.atZone(timeAccess.systemZone()).toLocalDateTime()
        pickTime(initialDateTime.toLocalDate(), initialDateTime.toLocalTime(), isRetry = false, onPicked)
    }

    private fun pickTime(date: LocalDate, initialTime: LocalTime, isRetry: Boolean, onPicked: (Instant) -> Unit) {
        val title = if (isRetry) {
            fragmentActivity.getString(R.string.taken_time_in_future)
        } else {
            fragmentActivity.getString(R.string.taken_on_date, timeFormatter.localDateToString(date))
        }
        timePickerDialogFactory.create(
            initialTime,
            title = title,
            negativeButtonText = R.string.change_date,
            onNegativeButtonClick = { minutes -> pickDate(date, minutes, onPicked) }
        ) { minutes ->
            confirm(date, minutes, onPicked)
        }.show(fragmentActivity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
    }

    /** OK and Cancel both return to the time picker with the clock at [minutes]; only OK changes the date in effect. */
    private fun pickDate(dateInEffect: LocalDate, minutes: Int, onPicked: (Instant) -> Unit) {
        val today = timeAccess.now().atZone(timeAccess.systemZone()).toLocalDate()
        val time = minutesToTime(minutes)
        datePickerDialogFactory.create(
            dateInEffect,
            maxDate = today,
            onCancel = { pickTime(dateInEffect, time, isRetry = false, onPicked) }
        ) { daysSinceEpoch ->
            pickTime(LocalDate.ofEpochDay(daysSinceEpoch), time, isRetry = false, onPicked)
        }.show(fragmentActivity.supportFragmentManager, DatePickerDialogFactory.DIALOG_TAG)
    }

    private fun confirm(date: LocalDate, minutes: Int, onPicked: (Instant) -> Unit) {
        val zone = timeAccess.systemZone()
        val now = timeAccess.now()
        val picked = takenTimeOrNull(date, minutes, zone, now)
        if (picked != null) {
            onPicked(picked)
        } else {
            // A future date cannot hold a valid time, so the retry moves to today.
            val nowDateTime = now.atZone(zone).toLocalDateTime()
            pickTime(minOf(date, nowDateTime.toLocalDate()), nowDateTime.toLocalTime(), isRetry = true, onPicked)
        }
    }
}

/** The instant of [minutes] past midnight on [date], or null when that instant lies after [now]. */
internal fun takenTimeOrNull(date: LocalDate, minutes: Int, zone: ZoneId, now: Instant): Instant? =
    date.atTime(minutesToTime(minutes)).atZone(zone).toInstant().takeIf { it <= now }

private fun minutesToTime(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)
