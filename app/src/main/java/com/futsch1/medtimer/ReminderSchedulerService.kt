package com.futsch1.medtimer

import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.reminders.ReminderProcessor.Companion.requestReschedule

class ReminderSchedulerService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()

        val medicineRepository = MedicineRepository(this.application)

        medicineRepository.liveMedicines.observe(
            this,
            Observer { _: List<FullMedicine> -> this.updateMedicine() })

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
        requestReschedule(this)
    }
}
