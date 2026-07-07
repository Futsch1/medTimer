package com.futsch1.medtimer.feature.ui.medicine

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.domain.model.Barcode
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.BarcodeRepository
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.integration.android.IntentIntegrator
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Scans a package barcode with the offline ZXing decoder and either restocks the medicine it is
 * already linked to, or (first scan of that code) asks which medicine to link it to.
 *
 * The two ActivityResultLaunchers are registered in the constructor rather than lazily, because
 * registerForActivityResult must be called unconditionally before the host fragment reaches
 * STARTED; that means this class has to be built during the fragment's onCreate, same as the
 * fragment's own launchers would be.
 */
class BarcodeScanner @AssistedInject constructor(
    @Assisted private val fragment: Fragment,
    private val medicineRepository: MedicineRepository,
    private val barcodeRepository: BarcodeRepository,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher,
    @param:Dispatcher(MedTimerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher
) {
    @AssistedFactory
    fun interface Factory {
        fun create(fragment: Fragment): BarcodeScanner
    }

    private val context get() = fragment.requireContext()

    private val requestCameraPermission = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchScan()
        } else {
            toast(context.getString(com.futsch1.medtimer.core.ui.R.string.barcode_camera_permission_denied))
        }
    }

    private val scanLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        val scanResult = IntentIntegrator.parseActivityResult(
            IntentIntegrator.REQUEST_CODE, activityResult.resultCode, activityResult.data
        )
        val barcode = scanResult?.contents
        if (barcode != null) {
            handleScannedBarcode(barcode)
        }
    }

    fun scan() {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            launchScan()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchScan() {
        val scanIntent = IntentIntegrator(fragment.requireActivity())
            .setDesiredBarcodeFormats(IntentIntegrator.PRODUCT_CODE_TYPES)
            .setOrientationLocked(false)
            .setBeepEnabled(true)
            .setPrompt(context.getString(com.futsch1.medtimer.core.ui.R.string.scan_barcode_prompt))
            .createScanIntent()
        scanLauncher.launch(scanIntent)
    }

    private fun handleScannedBarcode(barcode: String) {
        fragment.lifecycleScope.launch(dispatcher) {
            val medicineId = barcodeRepository.findMedicineId(barcode)
            if (medicineId != null) {
                refillAndNotify(medicineId)
            } else {
                val medicines = medicineRepository.getAll()
                withContext(mainDispatcher) { showLinkDialog(barcode, medicines) }
            }
        }
    }

    private suspend fun refillAndNotify(medicineId: Int) {
        val medicine = medicineRepository.fetch(medicineId) ?: return
        medicineRepository.increaseStock(medicineId, medicine.refillSize)
        withContext(mainDispatcher) {
            toast(context.getString(com.futsch1.medtimer.core.ui.R.string.barcode_stock_refilled, medicine.name))
        }
    }

    private fun showLinkDialog(barcode: String, medicines: List<Medicine>) {
        if (medicines.isEmpty()) {
            toast(context.getString(com.futsch1.medtimer.core.ui.R.string.barcode_no_medicines))
            return
        }
        val names: Array<CharSequence> = medicines.map { it.name as CharSequence }.toTypedArray()
        MaterialAlertDialogBuilder(context)
            .setTitle(com.futsch1.medtimer.core.ui.R.string.link_barcode_title)
            .setItems(names) { _, which -> linkAndRefill(barcode, medicines[which]) }
            .setNegativeButton(com.futsch1.medtimer.core.ui.R.string.cancel, null)
            .show()
    }

    private fun linkAndRefill(barcode: String, medicine: Medicine) {
        fragment.lifecycleScope.launch(dispatcher) {
            barcodeRepository.link(Barcode(barcode, medicine.id))
            medicineRepository.increaseStock(medicine.id, medicine.refillSize)
            withContext(mainDispatcher) {
                toast(
                    context.getString(
                        com.futsch1.medtimer.core.ui.R.string.barcode_linked_and_refilled, medicine.name
                    )
                )
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
