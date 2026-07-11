package com.futsch1.medtimer.feature.ui.medicine.ocr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

class MlKitPackageTextRecognizer @Inject constructor() : PackageTextRecognizer {
    override val isSupported: Boolean = true

    @OptIn(ExperimentalGetImage::class)
    override suspend fun recognize(image: ImageProxy): List<String> {
        val mediaImage = image.image ?: return emptyList()
        val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        return suspendCancellableCoroutine { continuation ->
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(inputImage)
                .addOnSuccessListener { text ->
                    continuation.resume(text.textBlocks.map { it.text }, onCancellation = null)
                }
                .addOnFailureListener { continuation.resume(emptyList(), onCancellation = null) }
        }
    }
}
