package com.futsch1.medtimer.feature.ui.statistics

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.addDividerToSpan
import com.futsch1.medtimer.core.common.helpers.addImageToSpan
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import com.futsch1.medtimer.feature.ui.overview.model.ScheduledReminderEvent
import com.futsch1.medtimer.feature.ui.overview.model.getImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

// Legacy XML CalendarFragment renderer: turns the shared calendar traversal (CalendarEventsProvider)
// into icon-bearing Spanned text. The Compose calendar renders CalendarDayEvent from the same
// traversal instead — see CalendarEventsProvider.getStructuredEvents.
@HiltViewModel
class CalendarEventsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val calendarEventsProvider: CalendarEventsProvider,
    private val reminderEventFactory: PastReminderEvent.Factory,
    private val scheduledReminderEventFactory: ScheduledReminderEvent.Factory,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val eventsByDay = MutableSharedFlow<Map<LocalDate, Spanned>>(replay = 1)

    fun getEventForMonths(
        medicineId: Int, pastMonths: Int, futureMonths: Int
    ): Flow<Map<LocalDate, Spanned>> {
        viewModelScope.launch(ioDispatcher) {
            val entries = calendarEventsProvider.getEntries(medicineId, pastMonths, futureMonths)
            eventsByDay.emit(entries.mapValues { (day, dayEntries) -> renderDay(day, dayEntries) })
        }
        return eventsByDay
    }

    private fun renderDay(day: LocalDate, entries: List<CalendarEntry>): Spanned {
        val builder = SpannableStringBuilder()
        if (entries.isNotEmpty()) {
            builder.append(day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))).append("\n")
        }
        entries.forEach { builder.append(it.toSpanned()).append("\n") }
        return builder
    }

    private fun CalendarEntry.toSpanned(): Spanned = when (this) {
        is CalendarEntry.Past -> reminderEventToString(event)
        is CalendarEntry.Future -> scheduledReminderToString(scheduledReminder)
    }

    private fun scheduledReminderToString(scheduledReminder: ScheduledReminder): Spanned {
        val builder = SpannableStringBuilder()
        addDividerToSpan(builder)
        return builder.append(scheduledReminderEventFactory.create(scheduledReminder).text)
    }

    private fun reminderEventToString(reminderEvent: ReminderEvent): Spanned {
        val overviewReminderEvent = reminderEventFactory.create(reminderEvent)
        val builder = SpannableStringBuilder()
        addDividerToSpan(builder)
        addImageToSpan(overviewReminderEvent.state.getImage(), builder, context)

        return builder.append(" ").append(overviewReminderEvent.text)
    }
}
