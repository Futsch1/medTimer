package com.futsch1.medtimer.feature.ui.medicine.scan

import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.MedicineLabel
import com.futsch1.medtimer.core.domain.repository.MedicineLabelRepository
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.ui.R as UiR
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Matches OCR text blocks recognized live from the camera against known medicines, and drives the
 * refill for confident matches. Kept across the lifetime of one scanning session (one
 * [PackageScanFragment] instance) so the same package isn't re-processed on every analyzed frame,
 * and so multiple distinct boxes shown to the camera in sequence each get handled once.
 *
 * Ambiguous cases (package matches no known medicine, or its pill count can't be parsed) show a
 * dialog and remember the answer as a [MedicineLabel], so the same package is recognized silently
 * next time.
 */
class PackageMatcher @AssistedInject constructor(
    @Assisted private val fragment: Fragment,
    private val medicineRepository: MedicineRepository,
    private val medicineLabelRepository: MedicineLabelRepository,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher,
    @param:Dispatcher(MedTimerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher
) {
    @AssistedFactory
    fun interface Factory {
        fun create(fragment: Fragment): PackageMatcher
    }

    private val context get() = fragment.requireContext()
    private val handledBlocks = mutableSetOf<String>()
    private val unmatchedCandidates = mutableMapOf<String, Int>()
    private val dialogOpen = AtomicBoolean(false)
    private val mutex = Mutex()

    /** Processes one frame's recognized text blocks. Safe to call repeatedly; never overlaps itself. */
    suspend fun handleBlocks(blocks: List<String>) {
        val normalized = blocks.map { normalize(it) }.filter { it.length >= MIN_BLOCK_LENGTH }
        if (normalized.isEmpty()) return

        mutex.withLock {
            val medicines = medicineRepository.getAll()
            val labels = medicineLabelRepository.getAll()
            for (block in normalized) {
                handleBlock(block, medicines, labels)
            }
        }
    }

    private suspend fun handleBlock(block: String, medicines: List<Medicine>, labels: List<MedicineLabel>) {
        if (block in handledBlocks) return

        val labelMatch = labels.firstOrNull { block.contains(normalize(it.text)) }
        if (labelMatch != null) {
            val medicine = medicines.firstOrNull { it.id == labelMatch.medicineId } ?: return
            val quantity = labelMatch.quantity
            if (quantity != null) {
                handledBlocks += block
                refill(medicine, quantity)
            } else if (dialogOpen.compareAndSet(false, true)) {
                handledBlocks += block
                askQuantity(medicine, block)
            }
            return
        }

        val nameMatches = medicines.filter { it.name.isNotBlank() && block.contains(normalize(it.name)) }
        when (nameMatches.size) {
            0 -> trackUnmatchedCandidate(block, medicines)
            1 -> {
                val medicine = nameMatches[0]
                val quantity = QuantityParser.parse(block)
                if (quantity != null) {
                    handledBlocks += block
                    medicineLabelRepository.remember(MedicineLabel(block, medicine.id, quantity))
                    refill(medicine, quantity)
                } else if (dialogOpen.compareAndSet(false, true)) {
                    handledBlocks += block
                    askQuantity(medicine, block)
                }
            }

            else -> if (dialogOpen.compareAndSet(false, true)) {
                handledBlocks += block
                askWhichMedicine(block, medicines)
            }
        }
    }

    private fun trackUnmatchedCandidate(block: String, medicines: List<Medicine>) {
        if (medicines.isEmpty() || block.length < MIN_UNMATCHED_BLOCK_LENGTH) return
        val hits = (unmatchedCandidates[block] ?: 0) + 1
        if (hits >= STABILITY_THRESHOLD) {
            if (dialogOpen.compareAndSet(false, true)) {
                unmatchedCandidates.remove(block)
                handledBlocks += block
                askWhichMedicine(block, medicines)
            }
        } else {
            unmatchedCandidates[block] = hits
        }
    }

    private suspend fun refill(medicine: Medicine, quantity: Double) {
        ReminderProcessorBroadcastReceiver.requestRefill(context, medicine.id, quantity)
        withContext(mainDispatcher) {
            toast(
                context.getString(
                    UiR.string.package_stock_refilled_amount,
                    medicine.name,
                    MedicineHelper.formatAmount(quantity, medicine.unit)
                )
            )
        }
    }

    private suspend fun askQuantity(medicine: Medicine, block: String) {
        withContext(mainDispatcher) {
            val input = EditText(context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setPadding(dpToPx(24), dpToPx(8), dpToPx(24), dpToPx(8))
            }
            MaterialAlertDialogBuilder(context)
                .setTitle(medicine.name)
                .setMessage(UiR.string.ocr_ask_quantity)
                .setView(input)
                .setPositiveButton(UiR.string.ok) { _, _ ->
                    dialogOpen.set(false)
                    val quantity = input.text.toString().toDoubleOrNull()
                    if (quantity != null && quantity > 0) {
                        fragment.lifecycleScope.launch(dispatcher) {
                            medicineLabelRepository.remember(MedicineLabel(block, medicine.id, quantity))
                            refill(medicine, quantity)
                        }
                    }
                }
                .setNegativeButton(UiR.string.cancel) { _, _ ->
                    dialogOpen.set(false)
                    handledBlocks.remove(block)
                }
                .setOnCancelListener {
                    dialogOpen.set(false)
                    handledBlocks.remove(block)
                }
                .show()
        }
    }

    private suspend fun askWhichMedicine(block: String, medicines: List<Medicine>) {
        withContext(mainDispatcher) {
            val names: Array<CharSequence> = medicines.map { it.name as CharSequence }.toTypedArray()
            MaterialAlertDialogBuilder(context)
                .setTitle(UiR.string.ocr_pick_medicine_title)
                .setItems(names) { _, which ->
                    dialogOpen.set(false)
                    val medicine = medicines[which]
                    fragment.lifecycleScope.launch(dispatcher) {
                        val quantity = QuantityParser.parse(block)
                        if (quantity != null) {
                            medicineLabelRepository.remember(MedicineLabel(block, medicine.id, quantity))
                            refill(medicine, quantity)
                        } else if (dialogOpen.compareAndSet(false, true)) {
                            askQuantity(medicine, block)
                        }
                    }
                }
                .setNegativeButton(UiR.string.cancel) { _, _ ->
                    dialogOpen.set(false)
                    handledBlocks.remove(block)
                }
                .setOnCancelListener {
                    dialogOpen.set(false)
                    handledBlocks.remove(block)
                }
                .show()
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()

    private fun normalize(text: String): String = text.lowercase().replace(Regex("\\s+"), " ").trim()

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val MIN_BLOCK_LENGTH = 4
        private const val MIN_UNMATCHED_BLOCK_LENGTH = 12
        private const val STABILITY_THRESHOLD = 2
    }
}
