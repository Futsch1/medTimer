package com.futsch1.medtimer.statistics.domain

import android.content.SharedPreferences
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.normalizeMedicineName
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import com.futsch1.medtimer.reminders.scheduling.SchedulingSimulator
import com.futsch1.medtimer.statistics.ui.calendar.CalendarDayEvent
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class GetCalendarEventsUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val sharedPreferences: SharedPreferences,
) {
    suspend operator fun invoke(
        medicineId: Int,
        pastMonths: Int,
        futureMonths: Int,
    ): ImmutableMap<LocalDate, List<CalendarDayEvent>> {
        val eventListByDay: MutableMap<LocalDate, MutableList<CalendarDayEvent>> = mutableMapOf()

        val currentDate = LocalDate.now()
        val pastDays = currentDate.toEpochDay() - currentDate.minusMonths(pastMonths.toLong())
            .withDayOfMonth(1).toEpochDay()
        val futureDays = if (futureMonths > 0) (currentDate.plusMonths(futureMonths.toLong() + 1)
            .withDayOfMonth(1).toEpochDay() - 1) - currentDate.toEpochDay()
        else 0

        withContext(Dispatchers.IO) {
            val reminderEvents = medicineRepository.getLastDaysReminderEvents(pastDays.toInt())
            var allMedicines = medicineRepository.medicines
            var medicine: Medicine? = null
            if (medicineId > 0) {
                medicine = medicineRepository.getOnlyMedicine(medicineId)
                allMedicines =
                    allMedicines.filter { it.medicine.medicineId == medicineId }
            }
            addPastEvents(eventListByDay, reminderEvents, medicine, pastDays)
            addFutureEvents(eventListByDay, allMedicines, reminderEvents, futureDays)
        }

        return eventListByDay.toImmutableMap()
    }

    private fun addFutureEvents(
        eventListByDay: MutableMap<LocalDate, MutableList<CalendarDayEvent>>,
        allMedicines: List<FullMedicine>,
        reminderEvents: List<ReminderEvent>,
        futureDays: Long,
    ) {
        val timeProvider = object : TimeAccess {
            override fun systemZone(): ZoneId = ZoneId.systemDefault()
            override fun localDate(): LocalDate = LocalDate.now()
            override fun now(): Instant = Instant.now()
        }
        val endDay = LocalDate.now().plusDays(futureDays)

        val schedulingSimulator =
            SchedulingSimulator(allMedicines, reminderEvents, timeProvider, sharedPreferences)

        schedulingSimulator.simulate { scheduledReminder: ScheduledReminder, scheduledDate: LocalDate, _: Double ->
            if (scheduledDate < endDay) {
                eventListByDay.getOrPut(scheduledDate) { mutableListOf() }
                    .add(scheduledReminder.toCalendarDayEvent())
            }
            scheduledDate < endDay
        }
    }

    private fun ScheduledReminder.toCalendarDayEvent() = CalendarDayEvent(
        time = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()),
        amount = reminder.amount,
        medicineName = medicine.medicine.name,
        status = CalendarDayEvent.Status.SCHEDULED,
    )

    private fun addPastEvents(
        eventListByDay: MutableMap<LocalDate, MutableList<CalendarDayEvent>>,
        reminderEvents: List<ReminderEvent>,
        medicine: Medicine?,
        pastDays: Long,
    ) {
        val startDay = LocalDate.now().minusDays(pastDays)
        val zone = ZoneId.systemDefault()
        for (reminderEvent in reminderEvents) {
            if (reminderEvent.status == ReminderEvent.ReminderStatus.DELETED) {
                continue
            }

            val day = Instant.ofEpochSecond(reminderEvent.remindedTimestamp)
                .atZone(zone).toLocalDate()
            if ((day >= startDay) && (medicine == null || medicine.name == normalizeMedicineName(
                    reminderEvent.medicineName
                ))
            ) {
                eventListByDay.getOrPut(day) { mutableListOf() }
                    .add(reminderEvent.toCalendarDayEvent())
            }
        }
    }

    private fun ReminderEvent.toCalendarDayEvent(): CalendarDayEvent {
        val zone = ZoneId.systemDefault()
        val timeStamp =
            if (processedTimestamp != 0L) processedTimestamp else remindedTimestamp

        val eventStatus = when (status) {
            ReminderEvent.ReminderStatus.SKIPPED -> CalendarDayEvent.Status.SKIPPED
            ReminderEvent.ReminderStatus.RAISED -> CalendarDayEvent.Status.RAISED
            else -> CalendarDayEvent.Status.TAKEN
        }

        return CalendarDayEvent(
            time = Instant.ofEpochSecond(timeStamp).atZone(zone).toLocalDateTime(),
            amount = amount,
            medicineName = medicineName,
            status = eventStatus,
        )
    }
}