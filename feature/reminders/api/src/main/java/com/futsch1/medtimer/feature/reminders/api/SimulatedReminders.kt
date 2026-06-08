package com.futsch1.medtimer.feature.reminders.api

import com.futsch1.medtimer.core.domain.model.SimulatedReminder
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * View of the simulated upcoming reminders for the UI layer.
 *
 * Consumers outside the reminders impl module depend on this interface instead of the concrete
 * [com.futsch1.medtimer.feature.reminders.impl.SimulatedRemindersRepository], which keeps feature:ui
 * decoupled from the impl module. The impl binds itself to this interface (see the reminders
 * impl module). Consumers may widen the simulation window they need, but triggering a
 * recalculation stays internal to the impl module.
 */
interface SimulatedReminders {
    val simulatedReminders: StateFlow<List<SimulatedReminder>>
    val simulatedThrough: StateFlow<LocalDate>
    val stockRunOutDates: StateFlow<Map<Int, LocalDate?>>

    /** Widens the simulated window to [days] for as long as [consumerId] holds it. */
    fun requestWindow(consumerId: String, days: Long)

    fun releaseWindow(consumerId: String)

    companion object {
        const val DEFAULT_SIMULATION_DAYS = 28L
    }
}
