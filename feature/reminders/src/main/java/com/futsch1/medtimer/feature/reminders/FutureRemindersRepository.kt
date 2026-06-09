package com.futsch1.medtimer.feature.reminders

import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.IdlingResourcesPool
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.scheduling.SchedulingSimulator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
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

    private val _simulatedThrough = MutableStateFlow(LocalDate.MIN)
    val simulatedThrough: StateFlow<LocalDate> = _simulatedThrough.asStateFlow()

    private val triggerChannel = Channel<LocalDate>(Channel.CONFLATED)

    private val idlingResource =
        IdlingResourcesPool.getInstance().getResource("FutureRemindersRepository")

    init {
        idlingResource.setIdle()
        applicationScope.launch {
            triggerChannel.consumeEach { endDay ->
                try {
                    val startTime = System.currentTimeMillis()
                    Log.d(
                        LogTags.SIMULATION,
                        "Triggering future reminders simulation through $endDay"
                    )
                    runSimulation(endDay)
                    _simulatedThrough.value = endDay
                    Log.d(
                        LogTags.SIMULATION,
                        "Future reminders simulation finished after ${System.currentTimeMillis() - startTime} ms"
                    )
                } catch (e: Exception) {
                    Log.e(LogTags.SIMULATION, "Future reminders simulation failed", e)
                } finally {
                    if (triggerChannel.isEmpty) {
                        idlingResource.setIdle()
                    }
                }
            }
        }
    }

    fun triggerCalculation(
        endDay: LocalDate = timeAccess.localDate().plusDays(DEFAULT_SIMULATION_DAYS)
    ) {
        // Never shrink the simulation window during a session
        val effectiveEndDay = maxOf(endDay, _simulatedThrough.value)
        Log.d(LogTags.SIMULATION, "Queuing future reminders simulation through $effectiveEndDay")
        idlingResource.setBusy()
        triggerChannel.trySend(effectiveEndDay)
    }

    private suspend fun runSimulation(endDay: LocalDate) {
        val medicines = medicineRepository.getAll()
        val reminderEvents = reminderEventRepository.getForScheduling(medicines)
        val result = mutableListOf<ScheduledReminder>()
        var currentEmitDay = LocalDate.MIN

        SchedulingSimulator(
            medicines,
            reminderEvents,
            timeAccess,
            preferencesDataSource
        ).simulate { scheduledReminder, scheduledDate, _ ->
            if (scheduledDate < endDay) {
                if (scheduledDate > currentEmitDay && currentEmitDay != LocalDate.MIN) {
                    _simulatedReminders.value = result.toList()
                }
                currentEmitDay = scheduledDate
                result.add(scheduledReminder)
            }
            scheduledDate < endDay
        }

        _simulatedReminders.value = result
    }

    companion object {
        const val DEFAULT_SIMULATION_DAYS = 28L
    }
}
