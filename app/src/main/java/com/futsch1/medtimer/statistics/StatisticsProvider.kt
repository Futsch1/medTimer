package com.futsch1.medtimer.statistics

import com.androidplot.xy.SimpleXYSeries
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import com.futsch1.medtimer.helpers.MedicineHelper.normalizeMedicineName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class StatisticsProvider @Inject constructor(private val medicineRepository: MedicineRepository) {
    companion object {
        private fun calculateDataEntries(
            days: Int,
            medicineToDayCount: Map<String, IntArray>
        ): List<SimpleXYSeries> {
            if (medicineToDayCount.isEmpty()) {
                return listOf()
            }

            val data = mutableListOf<SimpleXYSeries>()
            for ((name, counts) in medicineToDayCount) {
                val xValues = (days - 1 downTo 0).map { LocalDate.now().toEpochDay() - it }
                val yValues = (days - 1 downTo 0).map { counts.getOrElse(it) { 0 } }
                data.add(SimpleXYSeries(xValues, yValues, name))
            }
            return data
        }
    }

    suspend fun getTakenSkippedData(days: Int): TakenSkipped {
        val reminderEvents = medicineRepository.getAllReminderEventsWithoutDeleted()
        val taken = reminderEvents.count { eventStatusDaysFilter(it, days, ReminderStatus.TAKEN) }
        val skipped =
            reminderEvents.count { eventStatusDaysFilter(it, days, ReminderStatus.SKIPPED) }
        return TakenSkipped(taken.toLong(), skipped.toLong())
    }

    private fun eventStatusDaysFilter(
        event: ReminderEvent,
        days: Int,
        status: ReminderStatus
    ): Boolean {
        if (event.status != status) {
            return false
        }

        return days == 0 || wasAfter(
            event.remindedTimestamp,
            LocalDate.now().minusDays(days.toLong())
        )
    }

    private fun wasAfter(secondsSinceEpoch: Long, date: LocalDate): Boolean {
        val instant = Instant.ofEpochSecond(secondsSinceEpoch)
        return instant.atZone(ZoneId.systemDefault()).toLocalDate().isAfter(date)
    }

    suspend fun getLastDaysReminders(days: Int): List<SimpleXYSeries> {
        val medicineToDayCount = calculateMedicineToDayMap(days)
        return calculateDataEntries(days, medicineToDayCount)
    }

    private suspend fun calculateMedicineToDayMap(days: Int): Map<String, IntArray> {
        val earliestDate = LocalDate.now().minusDays(days.toLong())

        val reminderEvents = medicineRepository.getAllReminderEventsWithoutDeleted()
        return reminderEvents
            .filter {
                it.status == ReminderStatus.TAKEN && wasAfter(
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

    private fun getDaysInThePast(secondsSinceEpoch: Long): Int {
        val eventDate = Instant.ofEpochSecond(secondsSinceEpoch)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()
        return (today.toEpochDay() - eventDate.toEpochDay()).toInt()
    }

    data class TakenSkipped(val taken: Long, val skipped: Long)
}
