package com.futsch1.medtimer.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
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
        val fullMedicine: FullMedicine,
        val reminder: Reminder,
        val userPreferences: UserPreferences
    ) : ShowMedicineUiState
}


@HiltViewModel
class ShowMedicineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val medicineRepository: MedicineRepository,
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
            val reminder = medicineRepository.getReminder(reminderId)
            if (reminder == null) {
                _uiState.value = ShowMedicineUiState.NotFound
                return@launch
            }
            val fullMedicine = medicineRepository.getMedicine(reminder.medicineRelId)
            if (fullMedicine == null) {
                _uiState.value = ShowMedicineUiState.NotFound
                return@launch
            }
            _uiState.value = ShowMedicineUiState.Loaded(
                fullMedicine = fullMedicine,
                reminder = reminder,
                userPreferences = userPreferences
            )
        }
    }
}