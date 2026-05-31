package com.futsch1.medtimer.feature.ui.statistics

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.TimeHelper.secondsSinceEpochToLocalDate
import com.futsch1.medtimer.core.common.helpers.addDividerToSpan
import com.futsch1.medtimer.core.common.helpers.addImageToSpan
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.TimeAccess
import com.futsch1.medtimer.feature.reminders.scheduling.SchedulingSimulator
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import com.futsch1.medtimer.feature.ui.overview.model.ScheduledReminderEvent
import com.futsch1.medtimer.feature.ui.overview.model.getImage
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarDayEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@HiltViewModel
class CalendarEventsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val preferencesDataSource: PreferencesDataSource,
    private val reminderEventFactory: PastReminderEvent.Factory,
    private val scheduledReminderEventFactory: ScheduledReminderEvent.Factory,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private var reminderEvents: List<ReminderEvent> = listOf()
    private var allMedicines: List<Medicine> = listOf()
    private var medicine: Medicine? = null
    private val eventsByDay = MutableSharedFlow<Map<LocalDate, Spanned>>(replay = 1)
    private var eventListByDay: MutableMap<LocalDate, MutableList<Spanned>> = mutableMapOf()
    private val structuredEventsByDay =
        MutableSharedFlow<Map<LocalDate, List<CalendarDayEvent>>>(replay = 1)

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
            allMedicines = medicineRepository.getAll()
            if (medicineId > 0) {
                medicine = medicineRepository.fetch(medicineId)
                allMedicines =
                    allMedicines.filter { medicine -> medicine.id == medicineId }
            }
            addPastEvents(pastDays)
            addFutureEvents(futureDays)
            eventsByDay.emit(buildEventsByDay())
        }
        return eventsByDay
    }

    // Structured variant for the Compose calendar: emits typed events the DayEventsCard renders
    // theme-aware, instead of the icon-bearing Spanned used by the standalone CalendarFragment.
    fun getStructuredEventsForMonths(
        medicineId: Int,
        pastMonths: Int,
        futureMonths: Int,
    ): Flow<Map<LocalDate, List<CalendarDayEvent>>> {
        val currentDate = LocalDate.now()
        val pastDays = currentDate.toEpochDay() - currentDate.minusMonths(pastMonths.toLong())
            .withDayOfMonth(1).toEpochDay()
        val futureDays = if (futureMonths > 0) (currentDate.plusMonths(futureMonths.toLong() + 1)
            .withDayOfMonth(1).toEpochDay() - 1) - currentDate.toEpochDay()
        else 0

        viewModelScope.launch(ioDispatcher) {
            val events = reminderEventRepository.getLastDays(pastDays.toInt())
            var medicines = medicineRepository.getAll()
            var selectedMedicine: Medicine? = null
            if (medicineId > 0) {
                selectedMedicine = medicineRepository.fetch(medicineId)
                medicines = medicines.filter { it.id == medicineId }
            }

            val structured: MutableMap<LocalDate, MutableList<CalendarDayEvent>> = mutableMapOf()
            addStructuredPastEvents(structured, events, selectedMedicine, pastDays)
            addStructuredFutureEvents(structured, medicines, events, futureDays)
            structuredEventsByDay.emit(structured)
        }
        return structuredEventsByDay
    }

    private fun addStructuredPastEvents(
        target: MutableMap<LocalDate, MutableList<CalendarDayEvent>>,
        events: List<ReminderEvent>,
        selectedMedicine: Medicine?,
        pastDays: Long,
    ) {
        val startDay = LocalDate.now().minusDays(pastDays)
        val zone = ZoneId.systemDefault()
        for (reminderEvent in events) {
            if (reminderEvent.status == ReminderEvent.ReminderStatus.DELETED) {
                continue
            }
            val day = reminderEvent.remindedTimestamp.atZone(zone).toLocalDate()
            if (day >= startDay && (selectedMedicine == null ||
                    selectedMedicine.name == MedicineHelper.normalizeMedicineName(reminderEvent.medicineName))
            ) {
                target.getOrPut(day) { mutableListOf() }.add(reminderEvent.toCalendarDayEvent(zone))
            }
        }
    }

    private fun ReminderEvent.toCalendarDayEvent(zone: ZoneId): CalendarDayEvent {
        val timestamp =
            if (processedTimestamp != Instant.EPOCH) processedTimestamp else remindedTimestamp
        val eventStatus = when (status) {
            ReminderEvent.ReminderStatus.SKIPPED -> CalendarDayEvent.Status.SKIPPED
            ReminderEvent.ReminderStatus.RAISED -> CalendarDayEvent.Status.RAISED
            else -> CalendarDayEvent.Status.TAKEN
        }
        return CalendarDayEvent(
            time = LocalDateTime.ofInstant(timestamp, zone),
            amount = amount,
            medicineName = medicineName,
            status = eventStatus,
        )
    }

    private fun addStructuredFutureEvents(
        target: MutableMap<LocalDate, MutableList<CalendarDayEvent>>,
        medicines: List<Medicine>,
        events: List<ReminderEvent>,
        futureDays: Long,
    ) {
        if (futureDays <= 0) {
            return
        }
        val zone = ZoneId.systemDefault()
        val timeProvider = object : TimeAccess {
            override fun systemZone(): ZoneId = ZoneId.systemDefault()
            override fun localDate(): LocalDate = LocalDate.now()
            override fun now(): Instant = Instant.now()
        }
        val endDay = LocalDate.now().plusDays(futureDays)
        val schedulingSimulator =
            SchedulingSimulator(medicines, events, timeProvider, preferencesDataSource)

        schedulingSimulator.simulate { scheduledReminder: ScheduledReminder, scheduledDate: LocalDate, _: Double ->
            if (scheduledDate < endDay) {
                target.getOrPut(scheduledDate) { mutableListOf() }.add(
                    CalendarDayEvent(
                        time = LocalDateTime.ofInstant(scheduledReminder.timestamp, zone),
                        amount = scheduledReminder.reminder.amount,
                        medicineName = scheduledReminder.medicine.name,
                        status = CalendarDayEvent.Status.SCHEDULED,
                    )
                )
            }
            scheduledDate < endDay
        }
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
        val builder = SpannableStringBuilder()
        addDividerToSpan(builder)
        return builder.append(scheduledReminderEventFactory.create(scheduledReminder).text)
    }

    private fun addPastEvents(pastDays: Long) {
        val startDay = LocalDate.now().minusDays(pastDays)
        for (reminderEvent in reminderEvents) {
            if (reminderEvent.status == ReminderEvent.ReminderStatus.DELETED) {
                continue
            }

            val day = secondsSinceEpochToLocalDate(
                reminderEvent.remindedTimestamp.epochSecond,
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
        val overviewReminderEvent = reminderEventFactory.create(reminderEvent)
        val builder = SpannableStringBuilder()
        addDividerToSpan(builder)
        addImageToSpan(overviewReminderEvent.state.getImage(), builder, context)

        return builder.append(" ").append(overviewReminderEvent.text)
    }
}
