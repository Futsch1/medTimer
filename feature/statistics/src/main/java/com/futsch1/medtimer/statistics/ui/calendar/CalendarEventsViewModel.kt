package com.futsch1.medtimer.statistics.ui.calendar

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.normalizeMedicineName
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import com.futsch1.medtimer.reminders.scheduling.SchedulingSimulator
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

interface CalendarEventsState {
    val dayEvents: ImmutableMap<LocalDate, List<CalendarDayEvent>>
}

private class MutableCalendarEventsState : CalendarEventsState {
    override var dayEvents: ImmutableMap<LocalDate, List<CalendarDayEvent>> by mutableStateOf(
        persistentMapOf()
    )
}

class CalendarEventsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val medicineRepository = MedicineRepository(application)
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val _state = MutableCalendarEventsState()
    val state: CalendarEventsState get() = _state

    private var reminderEvents: List<ReminderEvent> = listOf()
    private var allMedicines: List<FullMedicine> = listOf()
    private var medicine: Medicine? = null

    suspend fun getEventForMonths(
        medicineId: Int, pastMonths: Int, futureMonths: Int
    ) {
        val eventListByDay: MutableMap<LocalDate, MutableList<CalendarDayEvent>> = mutableMapOf()

        // Calculate days in the past and the future based on the current date
        val currentDate = LocalDate.now()
        val pastDays = currentDate.toEpochDay() - currentDate.minusMonths(pastMonths.toLong())
            .withDayOfMonth(1).toEpochDay()
        val futureDays = if (futureMonths > 0) (currentDate.plusMonths(futureMonths.toLong() + 1)
            .withDayOfMonth(1).toEpochDay() - 1) - currentDate.toEpochDay()
        else 0

        withContext(Dispatchers.IO) {
            reminderEvents = medicineRepository.getLastDaysReminderEvents(pastDays.toInt())
            allMedicines = medicineRepository.medicines
            if (medicineId > 0) {
                medicine = medicineRepository.getOnlyMedicine(medicineId)
                allMedicines =
                    allMedicines.filter { medicine -> medicine.medicine.medicineId == medicineId }
            }
            addPastEvents(eventListByDay, pastDays)
            addFutureEvents(eventListByDay, futureDays)
        }

        _state.dayEvents = eventListByDay.toImmutableMap()
    }

    private fun addFutureEvents(
        eventListByDay: MutableMap<LocalDate, MutableList<CalendarDayEvent>>,
        futureDays: Long
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
                if (!eventListByDay.containsKey(scheduledDate)) {
                    eventListByDay[scheduledDate] = mutableListOf()
                }
                eventListByDay[scheduledDate]?.add(scheduledReminder.toCalendarDayEvent())
            }
            scheduledDate < endDay
        }
    }

    private fun ScheduledReminder.toCalendarDayEvent(): CalendarDayEvent {
        return CalendarDayEvent(
            time = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()),
            amount = reminder.amount,
            medicineName = medicine.medicine.name,
            status = CalendarDayEvent.Status.SCHEDULED,
        )
    }

    private fun addPastEvents(
        eventListByDay: MutableMap<LocalDate, MutableList<CalendarDayEvent>>,
        pastDays: Long
    ) {
        val startDay = LocalDate.now().minusDays(pastDays)
        val zone = ZoneId.systemDefault()
        for (reminderEvent: ReminderEvent in reminderEvents) {
            if (reminderEvent.status == ReminderEvent.ReminderStatus.DELETED) {
                continue
            }

            val day = Instant.ofEpochSecond(reminderEvent.remindedTimestamp)
                .atZone(zone).toLocalDate()
            if ((day >= startDay) && (medicine == null || medicine?.name == normalizeMedicineName(
                    reminderEvent.medicineName
                ))
            ) {
                if (!eventListByDay.containsKey(day)) {
                    eventListByDay[day] = mutableListOf()
                }
                eventListByDay[day]?.add(reminderEvent.toCalendarDayEvent())
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
