package com.futsch1.medtimer.feature.ui.medicine.ocr

import android.net.Uri

/**
 * Runs on-device OCR over a photo of a medicine package. Only implemented (via ML Kit) in the
 * "full" flavor, since ML Kit needs Google Play services; the "foss" flavor gets a no-op
 * implementation with [isSupported] = false, so callers can hide the feature entirely there
 * instead of showing a button that always fails.
 */
interface PackageTextRecognizer {
    val isSupported: Boolean

    /** Returns the recognized text (possibly blank), or blank if recognition failed. */
    suspend fun recognize(imageUri: Uri): String
}
