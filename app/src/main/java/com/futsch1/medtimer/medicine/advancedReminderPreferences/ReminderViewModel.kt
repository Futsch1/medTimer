package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.app.Application
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.EntityViewModel
import kotlinx.coroutines.flow.Flow

class ReminderViewModel(application: Application) : EntityViewModel<Reminder>(application) {
    override val medicineRepository = MedicineRepository(application)

    override fun getFlow(id: Int): Flow<Reminder?> = medicineRepository.getReminderFlow(id)
}
