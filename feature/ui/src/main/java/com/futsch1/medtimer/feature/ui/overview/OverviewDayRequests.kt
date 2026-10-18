package com.futsch1.medtimer.feature.ui.overview

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Days to show on the overview that were requested from outside of it, each delivered once. */
@Singleton
class OverviewDayRequests @Inject constructor() {
    // Buffered so a request survives until the overview is created; a channel, unlike a StateFlow, re-delivers an equal day.
    private val days = Channel<LocalDate>(Channel.CONFLATED)

    val requests: Flow<LocalDate> = days.receiveAsFlow()

    fun request(day: LocalDate) {
        days.trySend(day)
    }
}
