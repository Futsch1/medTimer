package com.futsch1.medtimer.feature.ui.statistics

import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ProcessedReminder
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.FutureRemindersRepository
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarDayEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

// A reminder pinned to a calendar day: a past event from the repository, or a future one the
// scheduler simulated. The map key carries the day; renderers read the source to produce their leaf.
sealed interface CalendarEntry {
    data class Past(val event: ReminderEvent) : CalendarEntry
    data class Future(val processedReminder: ProcessedReminder) : CalendarEntry
}

// The single calendar-month traversal: load events once, bucket past reminders and simulated future
// reminders by day. Callers collect entriesFlow and map to their own leaf — CalendarDayEvent for the
// Compose calendar (structuredEventsFlow) and the Spanned builder in CalendarEventsViewModel for the
// legacy XML CalendarFragment. They differ only at the leaf; windowing, filtering, and reactivity live here.
class CalendarEventsProvider @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val preferencesDataSource: PreferencesDataSource,
    private val futureRemindersRepository: FutureRemindersRepository,
) {

    // Combines a Room-backed past-events flow (re-emits on DB writes) with the simulated future
    // reminders flow. Both sources drive reactivity independently.
    fun entriesFlow(
        medicineId: Int,
        pastMonths: Int
    ): Flow<Map<LocalDate, List<CalendarEntry>>> {
        val zone = ZoneId.systemDefault()
        val startInstant = LocalDate.now()
            .minusMonths(pastMonths.toLong())
            .withDayOfMonth(1)
            .atStartOfDay(zone)
            .toInstant()

        return combine(
            reminderEventRepository.getAllFlow(
                startInstant,
                ReminderEvent.statusValuesWithoutDelete
            ),
            futureRemindersRepository.simulatedReminders,
            medicineRepository.getFlow(medicineId)
        ) { pastEvents, simulatedReminders, medicine ->
            buildEntriesByDay(pastEvents, simulatedReminders, medicine)
        }
    }

    // The Compose calendar's leaf: the typed CalendarDayEvent stream over the entries flow.
    fun structuredEventsFlow(
        medicineId: Int,
        pastMonths: Int
    ): Flow<Map<LocalDate, List<CalendarDayEvent>>> =
        entriesFlow(medicineId, pastMonths).map { it.toCalendarDayEvents() }

    suspend fun getStructuredEvents(
        medicineId: Int,
        pastMonths: Int
    ): Map<LocalDate, List<CalendarDayEvent>> =
        structuredEventsFlow(medicineId, pastMonths).first()

    private fun Map<LocalDate, List<CalendarEntry>>.toCalendarDayEvents(): Map<LocalDate, List<CalendarDayEvent>> =
        mapValues { (_, entries) -> entries.map { it.toCalendarDayEvent() } }

    private fun buildEntriesByDay(
        pastEvents: List<ReminderEvent>,
        simulatedReminders: List<ProcessedReminder>,
        medicine: Medicine?
    ): Map<LocalDate, List<CalendarEntry>> {
        val zone = ZoneId.systemDefault()

        val entriesByDay = LinkedHashMap<LocalDate, MutableList<CalendarEntry>>()

        for (reminderEvent in pastEvents) {
            if (medicine == null ||
                medicine.name == MedicineHelper.normalizeMedicineName(reminderEvent.medicineName)
            ) {
                val day = reminderEvent.remindedTimestamp.atZone(zone).toLocalDate()
                entriesByDay.getOrPut(day) { mutableListOf() }
                    .add(CalendarEntry.Past(reminderEvent))
            }
        }

        simulatedReminders
            .filter { processedReminder -> medicine == null || processedReminder.scheduledReminder.medicine.id == medicine.id }
            .forEach { processedReminder ->
                entriesByDay
                    .getOrPut(
                        processedReminder.scheduledReminder.timestamp.atZone(zone).toLocalDate()
                    ) { mutableListOf() }
                    .add(CalendarEntry.Future(processedReminder))
            }

        return entriesByDay
    }

    private fun CalendarEntry.toCalendarDayEvent(): CalendarDayEvent = when (this) {
        is CalendarEntry.Past -> event.toCalendarDayEvent()
        is CalendarEntry.Future -> processedReminder.scheduledReminder.toCalendarDayEvent()
    }

    private fun ReminderEvent.toCalendarDayEvent(): CalendarDayEvent {
        val zone = ZoneId.systemDefault()
        val eventStatus = when (status) {
            ReminderEvent.ReminderStatus.SKIPPED -> CalendarDayEvent.Status.SKIPPED
            ReminderEvent.ReminderStatus.RAISED -> CalendarDayEvent.Status.RAISED
            else -> CalendarDayEvent.Status.TAKEN
        }
        return CalendarDayEvent(
            time = LocalDateTime.ofInstant(remindedTimestamp, zone),
            amount = amount,
            medicineName = medicineName,
            status = eventStatus,
            reminderType = reminderType,
            takenTime = takenTime(zone),
            interval = interval(),
        )
    }

    // The processed (taken) time, shown after the arrow — only for taken events and only when the user
    // opted into seeing taken times, mirroring the legacy overview formatter's showTakenTimeInOverview gate.
    private fun ReminderEvent.takenTime(zone: ZoneId): LocalDateTime? {
        val isTaken =
            status == ReminderEvent.ReminderStatus.TAKEN || status == ReminderEvent.ReminderStatus.ACKNOWLEDGED
        val show = processedTimestamp.epochSecond != 0L && isTaken &&
                preferencesDataSource.preferences.value.showTakenTimeInOverview
        return if (show) LocalDateTime.ofInstant(processedTimestamp, zone) else null
    }

    // Elapsed time from the last interval reminder to when the dose was taken (interval-type reminders only),
    // matching the legacy formatter's getLastIntervalTime computation.
    private fun ReminderEvent.interval(): Duration? {
        val durationMillis =
            processedTimestamp.epochSecond * 1000L - lastIntervalReminderTimeInMinutes * 60_000L
        val show = lastIntervalReminderTimeInMinutes > 0 &&
                status == ReminderEvent.ReminderStatus.TAKEN && durationMillis >= 0
        return if (show) Duration.ofMillis(durationMillis) else null
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
