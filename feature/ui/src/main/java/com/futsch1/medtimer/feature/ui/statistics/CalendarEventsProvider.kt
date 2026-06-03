package com.futsch1.medtimer.feature.ui.statistics

import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.TimeAccess
import com.futsch1.medtimer.feature.reminders.scheduling.SchedulingSimulator
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarDayEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

// A reminder pinned to a calendar day: a past event from the repository, or a future one the
// scheduler simulated. The map key carries the day; renderers read the source to produce their leaf.
sealed interface CalendarEntry {
    data class Past(val event: ReminderEvent) : CalendarEntry
    data class Future(val scheduledReminder: ScheduledReminder) : CalendarEntry
}

// The single calendar-month traversal: load events once, bucket past reminders and simulated future
// reminders by day. Callers collect entriesFlow and map to their own leaf — CalendarDayEvent for the
// Compose calendar (structuredEventsFlow) and the Spanned builder in CalendarEventsViewModel for the
// legacy XML CalendarFragment. They differ only at the leaf; windowing, filtering, and reactivity live here.
class CalendarEventsProvider @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val preferencesDataSource: PreferencesDataSource,
) {

    // The calendar isn't backed by a reactive query — getEntries reads a fixed window on each call.
    // This adapts a change [trigger] (e.g. the screen's shared reminder-events flow) into the reactive
    // shape callers collect, re-reading the window on every emission (the trigger's value is ignored).
    // Both renderers map this one stream to their own leaf, so the provider owns the calendar's
    // reactivity instead of each caller faking it around a suspend read.
    fun entriesFlow(
        trigger: Flow<*>,
        medicineId: Int,
        pastMonths: Int,
        futureMonths: Int,
    ): Flow<Map<LocalDate, List<CalendarEntry>>> =
        trigger.map { getEntries(medicineId, pastMonths, futureMonths) }

    // The Compose calendar's leaf: the typed CalendarDayEvent stream over the entries flow.
    fun structuredEventsFlow(
        trigger: Flow<*>,
        medicineId: Int,
        pastMonths: Int,
        futureMonths: Int,
    ): Flow<Map<LocalDate, List<CalendarDayEvent>>> =
        entriesFlow(trigger, medicineId, pastMonths, futureMonths).map { it.toCalendarDayEvents() }

    suspend fun getStructuredEvents(
        medicineId: Int,
        pastMonths: Int,
        futureMonths: Int,
    ): Map<LocalDate, List<CalendarDayEvent>> =
        getEntries(medicineId, pastMonths, futureMonths).toCalendarDayEvents()

    private fun Map<LocalDate, List<CalendarEntry>>.toCalendarDayEvents(): Map<LocalDate, List<CalendarDayEvent>> =
        mapValues { (_, entries) -> entries.map { it.toCalendarDayEvent() } }

    suspend fun getEntries(
        medicineId: Int,
        pastMonths: Int,
        futureMonths: Int,
    ): Map<LocalDate, List<CalendarEntry>> {
        val currentDate = LocalDate.now()
        val pastDays = currentDate.toEpochDay() - currentDate.minusMonths(pastMonths.toLong())
            .withDayOfMonth(1).toEpochDay()
        val futureDays = if (futureMonths > 0) (currentDate.plusMonths(futureMonths.toLong() + 1)
            .withDayOfMonth(1).toEpochDay() - 1) - currentDate.toEpochDay()
        else 0

        val events = reminderEventRepository.getLastDays(pastDays.toInt())
        var medicines = medicineRepository.getAll()
        var selectedMedicine: Medicine? = null
        if (medicineId > 0) {
            selectedMedicine = medicineRepository.fetch(medicineId)
            medicines = medicines.filter { it.id == medicineId }
        }

        val entriesByDay = LinkedHashMap<LocalDate, MutableList<CalendarEntry>>()
        addPastEntries(entriesByDay, events, selectedMedicine, pastDays)
        addFutureEntries(entriesByDay, medicines, events, futureDays)
        return entriesByDay
    }

    private fun addPastEntries(
        target: MutableMap<LocalDate, MutableList<CalendarEntry>>,
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
                target.getOrPut(day) { mutableListOf() }.add(CalendarEntry.Past(reminderEvent))
            }
        }
    }

    private fun addFutureEntries(
        target: MutableMap<LocalDate, MutableList<CalendarEntry>>,
        medicines: List<Medicine>,
        events: List<ReminderEvent>,
        futureDays: Long,
    ) {
        if (futureDays <= 0) {
            return
        }
        val timeProvider = object : TimeAccess {
            override fun systemZone(): ZoneId = ZoneId.systemDefault()
            override fun localDate(): LocalDate = LocalDate.now()
            override fun now(): Instant = Instant.now()
        }
        val endDay = LocalDate.now().plusDays(futureDays)
        val schedulingSimulator = SchedulingSimulator(medicines, events, timeProvider, preferencesDataSource)

        schedulingSimulator.simulate { scheduledReminder: ScheduledReminder, scheduledDate: LocalDate, _: Double ->
            if (scheduledDate < endDay) {
                target.getOrPut(scheduledDate) { mutableListOf() }.add(CalendarEntry.Future(scheduledReminder))
            }
            scheduledDate < endDay
        }
    }

    private fun CalendarEntry.toCalendarDayEvent(): CalendarDayEvent = when (this) {
        is CalendarEntry.Past -> event.toCalendarDayEvent()
        is CalendarEntry.Future -> scheduledReminder.toCalendarDayEvent()
    }

    private fun ReminderEvent.toCalendarDayEvent(): CalendarDayEvent {
        val zone = ZoneId.systemDefault()
        val timestamp = if (processedTimestamp != Instant.EPOCH) processedTimestamp else remindedTimestamp
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
            reminderType = reminderType,
        )
    }

    private fun ScheduledReminder.toCalendarDayEvent(): CalendarDayEvent {
        val zone = ZoneId.systemDefault()
        return CalendarDayEvent(
            time = LocalDateTime.ofInstant(timestamp, zone),
            amount = reminder.amount,
            medicineName = medicine.name,
            status = CalendarDayEvent.Status.SCHEDULED,
            reminderType = reminder.reminderType,
        )
    }
}
