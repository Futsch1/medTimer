package com.futsch1.medtimer.feature.ui.medicine

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.MedicineLabel
import com.futsch1.medtimer.core.domain.repository.MedicineLabelRepository
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver
import com.futsch1.medtimer.feature.ui.medicine.ocr.PackageTextRecognizer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Photographs a medicine package with the system camera app and runs on-device OCR (ML Kit, only
 * present in the "full" flavor - see PackageTextRecognizer) over the result to figure out which
 * medicine to restock. Matches the recognized text first against previously remembered snippets
 * (MedicineLabel, for cases where the box text doesn't obviously contain the medicine's app
 * name), then against every medicine's own name. Exactly one match -> refill directly. Anything
 * else -> ask, and remember the pick for next time.
 */
class PackageScanner @AssistedInject constructor(
    @Assisted private val fragment: Fragment,
    private val medicineRepository: MedicineRepository,
    private val medicineLabelRepository: MedicineLabelRepository,
    private val textRecognizer: PackageTextRecognizer,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher,
    @param:Dispatcher(MedTimerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher
) {
    @AssistedFactory
    fun interface Factory {
        fun create(fragment: Fragment): PackageScanner
    }

    val isSupported: Boolean get() = textRecognizer.isSupported

    private val context get() = fragment.requireContext()
    private var photoUri: Uri? = null

    private val captureLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = photoUri
        if (success && uri != null) {
            handleCapturedPhoto(uri)
        }
    }

    fun scan() {
        val photoFile = File(context.cacheDir, "package_scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        photoUri = uri
        captureLauncher.launch(uri)
    }

    private fun handleCapturedPhoto(uri: Uri) {
        fragment.lifecycleScope.launch(dispatcher) {
            val recognized = normalize(textRecognizer.recognize(uri))
            if (recognized.isBlank()) {
                withContext(mainDispatcher) {
                    toast(context.getString(com.futsch1.medtimer.core.ui.R.string.ocr_no_text_found))
                }
                return@launch
            }

            val medicines = medicineRepository.getAll()
            val labels = medicineLabelRepository.getAll()
            val labelMatches = labels.filter { recognized.contains(normalize(it.text)) }.map { it.medicineId }
            val nameMatches = medicines.filter { recognized.contains(normalize(it.name)) }.map { it.id }
            val matchedIds = (labelMatches + nameMatches).distinct()

            when (matchedIds.size) {
                1 -> refillAndNotify(matchedIds[0])
                else -> withContext(mainDispatcher) { showPickerDialog(recognized, medicines) }
            }
        }
    }

    private fun normalize(text: String): String = text.lowercase().replace(Regex("\\s+"), " ").trim()

    private suspend fun refillAndNotify(medicineId: Int) {
        val medicine = medicineRepository.fetch(medicineId) ?: return
        ReminderProcessorBroadcastReceiver.requestRefill(context, medicineId)
        withContext(mainDispatcher) {
            toast(context.getString(com.futsch1.medtimer.core.ui.R.string.package_stock_refilled, medicine.name))
        }
    }

    private fun showPickerDialog(recognizedText: String, medicines: List<Medicine>) {
        if (medicines.isEmpty()) {
            toast(context.getString(com.futsch1.medtimer.core.ui.R.string.ocr_no_medicines))
            return
        }
        val names: Array<CharSequence> = medicines.map { it.name as CharSequence }.toTypedArray()
        MaterialAlertDialogBuilder(context)
            .setTitle(com.futsch1.medtimer.core.ui.R.string.ocr_pick_medicine_title)
            .setItems(names) { _, which -> rememberAndRefill(recognizedText, medicines[which]) }
            .setNegativeButton(com.futsch1.medtimer.core.ui.R.string.cancel, null)
            .show()
    }

    private fun rememberAndRefill(recognizedText: String, medicine: Medicine) {
        fragment.lifecycleScope.launch(dispatcher) {
            medicineLabelRepository.remember(MedicineLabel(recognizedText, medicine.id))
            ReminderProcessorBroadcastReceiver.requestRefill(context, medicine.id)
            withContext(mainDispatcher) {
                toast(
                    context.getString(
                        com.futsch1.medtimer.core.ui.R.string.ocr_remembered_and_refilled, medicine.name
                    )
                )
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
