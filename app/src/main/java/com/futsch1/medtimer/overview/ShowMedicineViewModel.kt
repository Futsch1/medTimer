package com.futsch1.medtimer.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.ReminderSummaryFormatter
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ShowMedicineUiState {
    data object Loading : ShowMedicineUiState
    data object NotFound : ShowMedicineUiState
    data class Loaded(
        val medicine: Medicine,
        val reminder: Reminder,
        val reminderSummaryText: String,
        val userPreferences: UserPreferences
    ) : ShowMedicineUiState
}

@HiltViewModel
class ShowMedicineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderSummaryFormatter: ReminderSummaryFormatter,
    preferencesDataSource: PreferencesDataSource,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    companion object {
        const val ARG_REMINDER_ID = "reminder_id"
    }

    private val reminderId: Int = checkNotNull(savedStateHandle[ARG_REMINDER_ID])

    private val _uiState = MutableStateFlow<ShowMedicineUiState>(ShowMedicineUiState.Loading)
    val uiState: StateFlow<ShowMedicineUiState> = _uiState.asStateFlow()

    init {
        val userPreferences = preferencesDataSource.preferences.value
        viewModelScope.launch(ioDispatcher) {
            val reminder = reminderRepository.get(reminderId)
            if (reminder == null) {
                _uiState.value = ShowMedicineUiState.NotFound
                return@launch
            }
            val medicine = medicineRepository.get(reminder.medicineRelId)
            if (medicine == null) {
                _uiState.value = ShowMedicineUiState.NotFound
                return@launch
            }
            val summaryText = reminderSummaryFormatter.formatReminderSummary(reminder)
            _uiState.value = ShowMedicineUiState.Loaded(
                medicine = medicine,
                reminder = reminder,
                reminderSummaryText = summaryText,
                userPreferences = userPreferences
            )
        }
    }
}