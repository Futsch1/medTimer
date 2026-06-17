package com.futsch1.medtimer.feature.ui.medicine

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable


data class StockState(
    val stockString: String?,
    val stockWarning: Boolean,
    val stockRunOutDate: String?
)

@Immutable
data class MedicineUiState(
    val id: Int,
    val name: String,
    val reminderTimes: List<String>,
    val stockState: StockState,
    val icon: Bitmap?,
    val color: Int?,
    val tags: List<String>,
    val inactive: Boolean = false
)
