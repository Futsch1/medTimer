package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futsch1.medtimer.core.ui.component.MedTimerTimePickerDialog

/** Asks for the time of the reschedule pending in [viewModel]; shows nothing while none is pending. */
@Composable
fun RescheduleDialog(viewModel: RescheduleViewModel = hiltViewModel()) {
    val request by viewModel.request.collectAsStateWithLifecycle()

    request?.let { pending ->
        // A new request starts a fresh picker instead of reusing the previous one's state.
        key(pending) {
            MedTimerTimePickerDialog(
                initialTime = pending.initialTime,
                onConfirm = viewModel::confirm,
                onDismissRequest = viewModel::cancel,
            )
        }
    }
}
