package com.futsch1.medtimer.feature.ui.medicine

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class MedicineUiState(
    val name: String,
    val reminderTimes: List<String>,
    val stockRunOutDate: String?,
    val icon: Bitmap?,
    val tags: List<String>
)
