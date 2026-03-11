package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.app.Application
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.EntityViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    application: Application,
    override val medicineRepository: MedicineRepository
) : EntityViewModel<Reminder>(application) {

    override fun getFlow(id: Int): Flow<Reminder?> = medicineRepository.getReminderFlow(id)
}
