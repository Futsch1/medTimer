package com.futsch1.medtimer.feature.ui.statistics

import com.futsch1.medtimer.core.common.helpers.MedicineHelper.normalizeMedicineName
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class StatisticsProvider @Inject constructor(private val reminderEventRepository: ReminderEventRepository) {

    suspend fun getTakenSkippedData(days: Int): TakenSkipped {
        val reminderEvents = reminderEventRepository.getAllWithoutDeleted()
        val taken = reminderEvents.count { eventStatusDaysFilter(it, days, ReminderEvent.ReminderStatus.TAKEN) }
        val skipped =
            reminderEvents.count { eventStatusDaysFilter(it, days, ReminderEvent.ReminderStatus.SKIPPED) }
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

    suspend fun getLastDaysReminders(days: Int): MedicinePerDayData {
        val medicineToDayCount = calculateMedicineToDayMap(days)
        return calculateDataEntries(days, medicineToDayCount)
    }

    private fun calculateDataEntries(
        days: Int,
        medicineToDayCount: Map<String, IntArray>
    ): MedicinePerDayData {
        if (medicineToDayCount.isEmpty()) {
            return MedicinePerDayData(emptyList(), emptyList())
        }

        val today = LocalDate.now().toEpochDay()
        val epochDays = (days - 1 downTo 0).map { today - it }
        val series = medicineToDayCount.map { (name, counts) ->
            MedicineDaySeries(name, (days - 1 downTo 0).map { counts.getOrElse(it) { 0 } })
        }
        return MedicinePerDayData(epochDays, series)
    }

    private suspend fun calculateMedicineToDayMap(days: Int): Map<String, IntArray> {
        val earliestDate = LocalDate.now().minusDays(days.toLong())

        val reminderEvents = reminderEventRepository.getAllWithoutDeleted()
        return reminderEvents
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
