package com.futsch1.medtimer.statistics

import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper.secondsSinceEpochToLocalDate
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import com.futsch1.medtimer.reminders.scheduling.SchedulingSimulator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@HiltViewModel
class CalendarEventsViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val preferencesDataSource: PreferencesDataSource,
    private val reminderEventFactory: OverviewReminderEvent.Factory,
    private val scheduledReminderEventFactory: OverviewScheduledReminderEvent.Factory,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private var reminderEvents: List<ReminderEvent> = listOf()
    private var allMedicines: List<FullMedicine> = listOf()
    private var medicine: Medicine? = null
    private val eventsByDay = MutableSharedFlow<Map<LocalDate, Spanned>>(replay = 1)
    private var eventListByDay: MutableMap<LocalDate, MutableList<Spanned>> = mutableMapOf()

    fun getEventForMonths(
        medicineId: Int, pastMonths: Int, futureMonths: Int

    ): Flow<Map<LocalDate, Spanned>> {
        eventListByDay.clear()

        // Calculate days in the past and the future based on the current date
        val currentDate = LocalDate.now()
        val pastDays = currentDate.toEpochDay() - currentDate.minusMonths(pastMonths.toLong())
            .withDayOfMonth(1).toEpochDay()
        val futureDays = if (futureMonths > 0) (currentDate.plusMonths(futureMonths.toLong() + 1)
            .withDayOfMonth(1).toEpochDay() - 1) - currentDate.toEpochDay()
        else 0

        viewModelScope.launch(ioDispatcher) {
            reminderEvents = reminderEventRepository.getLastDays(pastDays.toInt())
            allMedicines = medicineRepository.getFullAll()
            if (medicineId > 0) {
                medicine = medicineRepository.get(medicineId)
                allMedicines =
                    allMedicines.filter { medicine -> medicine.medicine.medicineId == medicineId }
            }
            addPastEvents(pastDays)
            addFutureEvents(futureDays)
            eventsByDay.emit(buildEventsByDay())
        }
        return eventsByDay
    }

    private fun buildEventsByDay(): Map<LocalDate, Spanned> {
        val eventsByDayStrings: MutableMap<LocalDate, Spanned> = mutableMapOf()
        for (day in eventListByDay.keys) {
            eventListByDay[day]?.let { eventsByDayStrings[day] = buildDayEvents(day, it) }
        }
        return eventsByDayStrings
    }

    private fun buildDayEvents(day: LocalDate, eventStrings: List<Spanned>): Spanned {
        val builder = SpannableStringBuilder()
        if (eventStrings.isNotEmpty()) {
            builder.append(day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                .append("\n")
        }
        eventStrings.forEach { builder.append(it).append("\n") }

        return builder
    }

    private fun addFutureEvents(futureDays: Long) {
        val timeProvider = object : TimeAccess {
            override fun systemZone(): ZoneId = ZoneId.systemDefault()
            override fun localDate(): LocalDate = LocalDate.now()
            override fun now(): Instant = Instant.now()
        }
        val endDay = LocalDate.now().plusDays(futureDays)

        val schedulingSimulator = SchedulingSimulator(
            allMedicines,
            reminderEvents,
            timeProvider,
            preferencesDataSource
        )

        schedulingSimulator.simulate { scheduledReminder: ScheduledReminder, scheduledDate: LocalDate, _: Double ->
            if (scheduledDate < endDay) {
                eventListByDay.getOrPut(scheduledDate) { mutableListOf() }
                    .add(scheduledReminderToString(scheduledReminder))
            }
            scheduledDate < endDay
        }
    }

    private fun scheduledReminderToString(scheduledReminder: ScheduledReminder): Spanned {
        return scheduledReminderEventFactory.create(scheduledReminder).text
    }


    private fun addPastEvents(pastDays: Long) {
        val startDay = LocalDate.now().minusDays(pastDays)
        for (reminderEvent: ReminderEvent in reminderEvents) {
            if (reminderEvent.status == ReminderEvent.ReminderStatus.DELETED) {
                continue
            }

            val day = secondsSinceEpochToLocalDate(
                reminderEvent.remindedTimestamp,
                ZoneId.systemDefault()
            )
            if ((day >= startDay) && (medicine == null || medicine?.name == MedicineHelper.normalizeMedicineName(
                    reminderEvent.medicineName
                ))
            ) {
                eventListByDay.getOrPut(day) { mutableListOf() }
                    .add(reminderEventToString(reminderEvent))
            }
        }
    }

    private fun reminderEventToString(reminderEvent: ReminderEvent): Spanned {
        return reminderEventFactory.create(reminderEvent).text
    }
}
