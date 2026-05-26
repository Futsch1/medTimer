package com.futsch1.medtimer.feature.reminders

import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.feature.reminders.command.ReminderCommandBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderSchedulerService : LifecycleService() {
    @Inject
    lateinit var medicineRepository: MedicineRepository

    @Inject
    lateinit var commandBus: ReminderCommandBus

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            medicineRepository.getAllFlow().collect { updateMedicine() }
        }

        Log.i(LogTags.SCHEDULER, "Scheduler service created")
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        scheduleRequest()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.i(LogTags.SCHEDULER, "Scheduler service destroyed")
    }

    fun updateMedicine() {
        scheduleRequest()
    }

    private fun scheduleRequest() {
        applicationScope.launch {
            commandBus.scheduleNextNotification()
        }
    }
}
