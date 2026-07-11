package com.futsch1.medtimer.feature.ui.medicine.ocr

import androidx.camera.core.ImageProxy
import javax.inject.Inject

class NoOpPackageTextRecognizer @Inject constructor() : PackageTextRecognizer {
    override val isSupported: Boolean = false
    override suspend fun recognize(image: ImageProxy): List<String> = emptyList()
}
