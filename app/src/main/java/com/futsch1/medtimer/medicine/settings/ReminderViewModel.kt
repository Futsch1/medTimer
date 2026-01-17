package com.futsch1.medtimer.medicine.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import kotlinx.coroutines.flow.Flow

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    @JvmField
    val medicineRepository = MedicineRepository(application)

    fun getReminderFlow(reminderId: Int): Flow<Reminder?> = medicineRepository.getReminderFlow(reminderId)
}
