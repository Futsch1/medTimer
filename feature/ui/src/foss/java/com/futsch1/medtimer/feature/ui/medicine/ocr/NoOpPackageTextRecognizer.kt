package com.futsch1.medtimer.feature.ui.medicine.ocr

import android.net.Uri
import javax.inject.Inject

class NoOpPackageTextRecognizer @Inject constructor() : PackageTextRecognizer {
    override val isSupported: Boolean = false
    override suspend fun recognize(imageUri: Uri): String = ""
}
