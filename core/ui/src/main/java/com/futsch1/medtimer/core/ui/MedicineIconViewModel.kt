package com.futsch1.medtimer.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MedicineIconViewModel @Inject constructor(
    private val medicineIcons: MedicineIcons,
) : ViewModel() {
    fun getIconBitmap(iconId: Int): ImageBitmap = medicineIcons.getIconBitmapUntinted(iconId).asImageBitmap()
}

@Composable
fun rememberMedicineIcon(iconId: Int): ImageBitmap? {
    if (iconId == 0) {
        return null
    }

    val viewModel: MedicineIconViewModel = hiltViewModel()
    return remember(iconId) { viewModel.getIconBitmap(iconId) }
}
