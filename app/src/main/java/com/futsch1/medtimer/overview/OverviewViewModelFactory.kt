package com.futsch1.medtimer.overview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.preferences.MedTimerPreferencesDataSource

class OverviewViewModelFactory(
    context: Context,
    private val preferencesDataSource: MedTimerPreferencesDataSource,
    private val medicineViewModel: MedicineViewModel
) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OverviewViewModel::class.java)) {
            return OverviewViewModel(applicationContext, preferencesDataSource, medicineViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}
