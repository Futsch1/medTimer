package com.futsch1.medtimer.feature.ui.medicine.ocr

import androidx.camera.core.ImageProxy

/**
 * Runs on-device OCR over a live camera frame of a medicine package. Only implemented (via ML
 * Kit) in the "full" flavor, since ML Kit needs Google Play services; the "foss" flavor gets a
 * no-op implementation with [isSupported] = false, so callers can hide the feature entirely there
 * instead of showing a button that always fails.
 *
 * CameraX itself (unlike ML Kit) has no Google Play services dependency, so [ImageProxy] can be
 * referenced from this shared interface without leaking Google-only types into the "foss" build.
 */
interface PackageTextRecognizer {
    val isSupported: Boolean

    /**
     * Returns each recognized block of text separately (e.g. one per medicine box visible in
     * frame) so callers can match/act on them independently. Does not close [image]; the caller
     * owns its lifecycle. Returns an empty list if nothing was recognized or recognition failed.
     */
    suspend fun recognize(image: ImageProxy): List<String>
}
