package com.futsch1.medtimer.statistics.ui.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.futsch1.medtimer.statistics.domain.GetCalendarEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import java.time.LocalDate
import javax.inject.Inject

interface CalendarEventsState {
    val dayEvents: ImmutableMap<LocalDate, List<CalendarDayEvent>>
}

private class MutableCalendarEventsState : CalendarEventsState {
    override var dayEvents: ImmutableMap<LocalDate, List<CalendarDayEvent>> by mutableStateOf(
        persistentMapOf()
    )
}

@HiltViewModel
class CalendarEventsViewModel @Inject constructor(
    private val getCalendarEvents: GetCalendarEventsUseCase,
) : ViewModel() {

    private val _state = MutableCalendarEventsState()
    val state: CalendarEventsState get() = _state

    suspend fun getEventForMonths(
        medicineId: Int,
        pastMonths: Int,
        futureMonths: Int
    ) {
        _state.dayEvents = getCalendarEvents(medicineId, pastMonths, futureMonths)
    }
}