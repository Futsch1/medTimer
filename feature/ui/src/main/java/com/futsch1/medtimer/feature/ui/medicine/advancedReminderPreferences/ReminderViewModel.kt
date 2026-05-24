package com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences

import com.futsch1.medtimer.core.common.helpers.ModelDataViewModel
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ModelDataViewModel<Reminder>() {

    override fun getFlow(id: Int): Flow<Reminder?> = reminderRepository.getFlow(id)
}
