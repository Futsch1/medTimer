package com.futsch1.medtimer.feature.ui.medicine.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import javax.inject.Inject

class MlKitPackageTextRecognizer @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PackageTextRecognizer {
    override val isSupported: Boolean = true

    override suspend fun recognize(imageUri: Uri): String = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                .addOnSuccessListener { continuation.resume(it.text) }
                .addOnFailureListener { continuation.resume("") }
        } catch (_: IOException) {
            continuation.resume("")
        }
    }
}
