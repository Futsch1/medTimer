package com.futsch1.medtimer.feature.ui.statistics

import com.futsch1.medtimer.core.common.helpers.MedicineHelper.normalizeMedicineName
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

// Pure aggregation over an already-loaded reminder-event list. Holds no repository, so a single
// events read in the ViewModel feeds every chart series at once — see StatisticsScreenViewModel.observeCharts.
class StatisticsProvider @Inject constructor() {

    fun aggregate(events: List<ReminderEvent>, days: Int): ChartsData = ChartsData(
        perDay = getLastDaysReminders(events, days),
        period = getTakenSkippedData(events, days),
        total = getTakenSkippedData(events, 0),
    )

    fun getTakenSkippedData(events: List<ReminderEvent>, days: Int): TakenSkipped {
        val taken = events.count { eventStatusDaysFilter(it, days, ReminderEvent.ReminderStatus.TAKEN) }
        val skipped = events.count { eventStatusDaysFilter(it, days, ReminderEvent.ReminderStatus.SKIPPED) }
        return TakenSkipped(taken.toLong(), skipped.toLong())
    }

    private fun eventStatusDaysFilter(
        event: ReminderEvent,
        days: Int,
        status: ReminderEvent.ReminderStatus
    ): Boolean {
        if (event.status != status) {
            return false
        }

        return days == 0 || wasAfter(
            event.remindedTimestamp,
            LocalDate.now().minusDays(days.toLong())
        )
    }

    private fun wasAfter(instant: Instant, date: LocalDate): Boolean {
        return instant.atZone(ZoneId.systemDefault()).toLocalDate().isAfter(date)
    }

    fun getLastDaysReminders(events: List<ReminderEvent>, days: Int): MedicinePerDayData {
        val medicineToDayCount = calculateMedicineToDayMap(events, days)
        return calculateDataEntries(days, medicineToDayCount)
    }

    private fun calculateDataEntries(
        days: Int,
        medicineToDayCount: Map<String, IntArray>
    ): MedicinePerDayData {
        // Emit the full date range even when there's no data, so the bar chart can still show the dates
        // (empty bars) instead of a blank card. The series stays empty when nothing was taken.
        val today = LocalDate.now().toEpochDay()
        val epochDays = (days - 1 downTo 0).map { today - it }
        val series = medicineToDayCount.map { (name, counts) ->
            MedicineDaySeries(name, (days - 1 downTo 0).map { counts.getOrElse(it) { 0 } })
        }
        return MedicinePerDayData(epochDays, series)
    }

    private fun calculateMedicineToDayMap(events: List<ReminderEvent>, days: Int): Map<String, IntArray> {
        val earliestDate = LocalDate.now().minusDays(days.toLong())

        return events
            .filter {
                it.status == ReminderEvent.ReminderStatus.TAKEN && wasAfter(
                    it.remindedTimestamp,
                    earliestDate
                )
            }
            .groupBy { normalizeMedicineName(it.medicineName) }
            .mapValues { (_, events) ->
                IntArray(days).also { array ->
                    events.forEach { event ->
                        val daysInThePast = getDaysInThePast(event.remindedTimestamp)
                        if (daysInThePast in 0 until array.size) {
                            array[daysInThePast]++
                        }
                    }
                }
            }
    }

    private fun getDaysInThePast(instant: Instant): Int {
        val eventDate = instant
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()
        return (today.toEpochDay() - eventDate.toEpochDay()).toInt()
    }

    data class TakenSkipped(val taken: Long, val skipped: Long)
}
