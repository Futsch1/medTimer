package com.futsch1.medtimer.medicine.advancedReminderPreferences

import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.helpers.EntityViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : EntityViewModel<ReminderEntity>() {

    override fun getFlow(id: Int): Flow<ReminderEntity?> = reminderRepository.getFlow(id)
}
