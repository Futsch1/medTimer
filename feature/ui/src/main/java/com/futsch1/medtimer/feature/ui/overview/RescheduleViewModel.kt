package com.futsch1.medtimer.feature.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.feature.ui.overview.actions.RescheduleRequest
import com.futsch1.medtimer.feature.ui.overview.actions.Rescheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/** Holds the reschedule waiting for a picked time, so the time picker survives configuration changes. */
@HiltViewModel
class RescheduleViewModel @Inject constructor(
    private val rescheduler: Rescheduler,
) : ViewModel() {
    private val _request = MutableStateFlow<RescheduleRequest?>(null)
    val request: StateFlow<RescheduleRequest?> = _request.asStateFlow()

    fun request(request: RescheduleRequest) {
        _request.value = request
    }

    fun confirm(time: LocalTime) {
        val request = _request.value ?: return
        _request.value = null
        viewModelScope.launch { rescheduler.reschedule(request, time) }
    }

    fun cancel() {
        _request.value = null
    }
}
