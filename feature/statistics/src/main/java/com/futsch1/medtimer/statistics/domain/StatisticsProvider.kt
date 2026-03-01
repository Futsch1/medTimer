package com.futsch1.medtimer.statistics.domain

import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import com.futsch1.medtimer.helpers.normalizeMedicineName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StatisticsProvider(val medicineRepository: MedicineRepository) {
    private fun earliestEpochSeconds(days: Int): Long = LocalDate.now()
        .minusDays(days.toLong())
        .atStartOfDay(ZoneId.systemDefault())
        .toEpochSecond()

    fun getTakenSkippedData(days: Int): TakenSkipped {
        val cutoff = if (days == 0) 0L else earliestEpochSeconds(days)

        val taken = medicineRepository.allReminderEventsWithoutDeleted
            .count { event -> event.status == ReminderStatus.TAKEN && (cutoff == 0L || event.remindedTimestamp > cutoff) }
        val skipped = medicineRepository.allReminderEventsWithoutDeleted
            .count { event -> event.status == ReminderStatus.SKIPPED && (cutoff == 0L || event.remindedTimestamp > cutoff) }
        return TakenSkipped(taken, skipped)
    }

    fun getLastDaysReminders(days: Int): List<MedicinePerDaySeries> {
        val cutoff = earliestEpochSeconds(days)
        val todayEpochDay = LocalDate.now().toEpochDay()
        val zone = ZoneId.systemDefault()

        return medicineRepository.allReminderEventsWithoutDeleted
            .filter { it.status == ReminderStatus.TAKEN && it.remindedTimestamp > cutoff }
            .groupBy { normalizeMedicineName(it.medicineName) }
            .map { (name, events) ->
                val xValues = (days - 1 downTo 0).map { day -> todayEpochDay - day }
                val yValues = events
                    .map { event ->
                        val eventDay = Instant.ofEpochSecond(event.remindedTimestamp)
                            .atZone(zone).toLocalDate().toEpochDay()
                        (todayEpochDay - eventDay).toInt()
                    }
                    .filter { daysAgo -> daysAgo in 0..<days }
                    .groupingBy { daysAgo -> daysAgo }
                    .eachCount()
                    .let { countsByDay -> (days - 1 downTo 0).map { day -> countsByDay[day] ?: 0 } }
                MedicinePerDaySeries(name, xValues, yValues)
            }
    }
}
