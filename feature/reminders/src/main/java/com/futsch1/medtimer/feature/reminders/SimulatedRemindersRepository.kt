package com.futsch1.medtimer.feature.reminders

import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.IdlingResourcesPool
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.SimulatedReminder
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.scheduling.SchedulingSimulator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SimulatedRemindersRepository @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val preferencesDataSource: PreferencesDataSource,
    private val timeAccess: TimeAccess,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) {
    private val _simulatedReminders = MutableStateFlow<List<SimulatedReminder>>(emptyList())
    val simulatedReminders: StateFlow<List<SimulatedReminder>> = _simulatedReminders.asStateFlow()

    private val _simulatedThrough = MutableStateFlow(LocalDate.MIN)
    val simulatedThrough: StateFlow<LocalDate> = _simulatedThrough.asStateFlow()

    private val _stockRunOutDates = MutableStateFlow<Map<Int, LocalDate?>>(emptyMap())
    val stockRunOutDates: StateFlow<Map<Int, LocalDate?>> = _stockRunOutDates.asStateFlow()

    private val triggerChannel = Channel<LocalDate>(Channel.CONFLATED)

    private val consumerWindows = mutableMapOf<String, Long>()

    private val idlingResource =
        IdlingResourcesPool.getInstance().getResource("FutureRemindersRepository")

    init {
        idlingResource.setIdle()
        applicationScope.launch {
            triggerChannel.consumeEach { endDay ->
                try {
                    delay(1000.milliseconds)
                    Log.d(
                        LogTags.SIMULATION,
                        "Triggering future reminders simulation through $endDay"
                    )

                    runSimulation(endDay)
                    _simulatedThrough.value = endDay
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

    fun requestWindow(consumerId: String, days: Long) {
        val previousMax = effectiveWindowDays()
        consumerWindows[consumerId] = days
        val newMax = effectiveWindowDays()
        if (newMax > previousMax) {
            triggerCalculation(timeAccess.localDate().plusDays(newMax))
        }
    }

    fun releaseWindow(consumerId: String) {
        consumerWindows.remove(consumerId)
        // Window shrinkage takes effect on next natural trigger — no immediate re-simulation on release
    }

    private fun effectiveWindowDays(): Long =
        consumerWindows.values.maxOrNull() ?: DEFAULT_SIMULATION_DAYS

    fun triggerCalculation(
        endDay: LocalDate = timeAccess.localDate().plusDays(effectiveWindowDays())
    ) {
        Log.d(LogTags.SIMULATION, "Queuing future reminders simulation through $endDay")
        idlingResource.setBusy()
        triggerChannel.trySend(endDay)
    }

    private suspend fun runSimulation(endDay: LocalDate) {
        val medicines = medicineRepository.getAll()
        val reminderEvents = reminderEventRepository.getForScheduling(medicines)
        val result = mutableListOf<SimulatedReminder>()
        val runOutDates = mutableMapOf<Int, LocalDate?>()
        medicines.forEach { runOutDates[it.id] = if (it.isStockManagementActive()) LocalDate.MAX else null }
        var currentEmitDay = LocalDate.MIN
        val startTime = System.currentTimeMillis()

        SchedulingSimulator(
            medicines,
            reminderEvents,
            timeAccess,
            preferencesDataSource
        ).simulate { simulatedReminder, scheduledDate ->
            if (scheduledDate < endDay) {
                if (scheduledDate > currentEmitDay && currentEmitDay != LocalDate.MIN) {
                    _simulatedReminders.value = result.toList()
                }
                currentEmitDay = scheduledDate
                result.add(simulatedReminder)
                val medicineId = simulatedReminder.scheduledReminder.medicine.id
                if (runOutDates[medicineId] == LocalDate.MAX && simulatedReminder.stockAfter == 0.0) {
                    runOutDates[medicineId] = scheduledDate
                }
            }
            scheduledDate < endDay && System.currentTimeMillis() - startTime < 2_000
        }

        Log.d(
            LogTags.SIMULATION,
            "Future reminders simulation finished after ${System.currentTimeMillis() - startTime} ms"
        )

        _simulatedReminders.value = result.sortedBy { it.scheduledReminder.timestamp }
        _stockRunOutDates.value = runOutDates
    }

    companion object {
        const val DEFAULT_SIMULATION_DAYS = 28L
    }
}
