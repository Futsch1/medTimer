package com.futsch1.medtimer.feature.ui.statistics

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.lifecycle.ViewModel
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.addDividerToSpan
import com.futsch1.medtimer.core.common.helpers.addImageToSpan
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ProcessedReminder
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import com.futsch1.medtimer.feature.ui.overview.model.ProcessedReminderEvent
import com.futsch1.medtimer.feature.ui.overview.model.getImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

// Legacy XML CalendarFragment renderer: maps the provider's shared entriesFlow into icon-bearing
// Spanned text. The Compose calendar maps the same flow to CalendarDayEvent instead — see
// CalendarEventsProvider.structuredEventsFlow.
@HiltViewModel
class CalendarEventsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val calendarEventsProvider: CalendarEventsProvider,
    private val reminderEventFactory: PastReminderEvent.Factory,
    private val processedReminderEventFactory: ProcessedReminderEvent.Factory,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    fun getEventForMonths(
        medicineId: Int, pastMonths: Int
    ): Flow<Map<LocalDate, Spanned>> =
        calendarEventsProvider.entriesFlow(medicineId, pastMonths)
            .map { entriesByDay ->
                entriesByDay.mapValues { (day, dayEntries) ->
                    renderDay(
                        day,
                        dayEntries
                    )
                }
            }
            .flowOn(ioDispatcher)

    private fun renderDay(day: LocalDate, entries: List<CalendarEntry>): Spanned {
        val builder = SpannableStringBuilder()
        if (entries.isNotEmpty()) {
            builder.append(day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                .append("\n")
        }
        entries.forEach { builder.append(it.toSpanned()).append("\n") }
        return builder
    }

    private fun CalendarEntry.toSpanned(): Spanned = when (this) {
        is CalendarEntry.Past -> reminderEventToString(event)
        is CalendarEntry.Future -> processedReminderToString(processedReminder)
    }

    private fun processedReminderToString(processedReminder: ProcessedReminder): Spanned {
        val builder = SpannableStringBuilder()
        addDividerToSpan(builder)
        return builder.append(processedReminderEventFactory.create(processedReminder).text)
    }

    private fun reminderEventToString(reminderEvent: ReminderEvent): Spanned {
        val overviewReminderEvent = reminderEventFactory.create(reminderEvent)
        val builder = SpannableStringBuilder()
        addDividerToSpan(builder)
        addImageToSpan(overviewReminderEvent.state.getImage(), builder, context)

        return builder.append(" ").append(overviewReminderEvent.text)
    }
}
