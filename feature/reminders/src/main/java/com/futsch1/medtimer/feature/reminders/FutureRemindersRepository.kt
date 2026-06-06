package com.futsch1.medtimer.feature.reminders

import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.scheduling.SchedulingSimulator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class FutureRemindersRepository @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val preferencesDataSource: PreferencesDataSource,
    private val timeAccess: TimeAccess,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) {
    private val _simulatedReminders = MutableStateFlow<List<ScheduledReminder>>(emptyList())
    val simulatedReminders: StateFlow<List<ScheduledReminder>> = _simulatedReminders.asStateFlow()

    private val _simulatedThrough = MutableStateFlow(java.time.LocalDate.MIN)
    val simulatedThrough: StateFlow<java.time.LocalDate> = _simulatedThrough.asStateFlow()

    private var calculationJob: Job? = null

    fun triggerCalculation(
        endDay: java.time.LocalDate = timeAccess.localDate().plusDays(DEFAULT_SIMULATION_DAYS),
        immediate: Boolean = false
    ) {
        // Never shrink the simulation window during a session
        val effectiveEndDay = maxOf(endDay, _simulatedThrough.value)
        Log.d(LogTags.SIMULATION, "Triggering future reminders simulation through $effectiveEndDay")
        calculationJob?.cancel()
        calculationJob = applicationScope.launch {
            if (!immediate) delay(DEBOUNCE_MS)
            _simulatedReminders.value = runSimulation(effectiveEndDay)
            _simulatedThrough.value = effectiveEndDay
            Log.d(LogTags.SIMULATION, "Future reminders simulation finished")
        }
    }

    private suspend fun runSimulation(endDay: java.time.LocalDate): List<ScheduledReminder> {
        val medicines = medicineRepository.getAll()
        val reminderEvents = reminderEventRepository.getForScheduling(medicines)
        val result = mutableListOf<ScheduledReminder>()

        SchedulingSimulator(
            medicines,
            reminderEvents,
            timeAccess,
            preferencesDataSource
        ).simulate { scheduledReminder, scheduledDate, _ ->
            if (scheduledDate < endDay) {
                result.add(scheduledReminder)
            }
            scheduledDate < endDay
        }

        return result
    }

    companion object {
        private val DEBOUNCE_MS = 1000.milliseconds
        const val DEFAULT_SIMULATION_DAYS = 28L
    }
}
