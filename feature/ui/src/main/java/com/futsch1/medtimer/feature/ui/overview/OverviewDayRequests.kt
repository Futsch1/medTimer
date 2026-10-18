package com.futsch1.medtimer.feature.ui.overview

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** A day to show on the overview, with the [reminderEvents] on it to bring into view. */
data class OverviewDayRequest(val day: LocalDate, val reminderEvents: List<ReminderEvent> = emptyList())

/** Days to show on the overview that were requested from outside of it, each delivered once. */
@Singleton
class OverviewDayRequests @Inject constructor() {
    // Buffered so a request survives until the overview is created; a channel, unlike a StateFlow, re-delivers an equal day.
    private val days = Channel<OverviewDayRequest>(Channel.CONFLATED)

    val requests: Flow<OverviewDayRequest> = days.receiveAsFlow()

    fun request(request: OverviewDayRequest) {
        days.trySend(request)
    }
}
