package com.futsch1.medtimer

import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver.Companion.requestScheduleNextNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderSchedulerService : LifecycleService() {
    @Inject
    lateinit var medicineRepository: MedicineRepository

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            medicineRepository.getFullAllFlow().collect { updateMedicine() }
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
        requestScheduleNextNotification(this)
    }
}
