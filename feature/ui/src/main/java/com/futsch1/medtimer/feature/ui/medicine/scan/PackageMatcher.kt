package com.futsch1.medtimer.feature.ui.medicine.scan

import android.text.InputType
import android.widget.EditText
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
 * [PackageScanFragment] instance).
 *
 * A single package's text is OCR'd as several separate blocks (brand name, dosage, pack size,
 * manufacturer...), so this only ever considers ONE candidate block at a time: the instant any
 * block resolves to a confident or ambiguous-but-recognized match, [handleBlocks] stops looking at
 * anything else until [resumeScanning] is called. This is what keeps stray fragments of an
 * already-handled package from being mistaken for a second, unknown package a frame or two later.
 *
 * A package that matches nothing gets a few seconds of patience (real elapsed time, not a count of
 * identical re-reads - OCR text is rarely byte-identical frame to frame, e.g. while the camera is
 * still focusing) before the "couldn't recognize this package" picker shows, using the longest
 * candidate block seen during that window.
 *
 * Ambiguous cases (package matches no known medicine, or its pill count can't be parsed) show a
 * dialog and remember the answer as a [MedicineLabel], so the same package is recognized silently
 * next time.
 */
class PackageMatcher @AssistedInject constructor(
    @Assisted private val fragment: Fragment,
    @Assisted private val onRecognized: (String) -> Unit,
    private val medicineRepository: MedicineRepository,
    private val medicineLabelRepository: MedicineLabelRepository,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher,
    @param:Dispatcher(MedTimerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher
) {
    @AssistedFactory
    fun interface Factory {
        fun create(fragment: Fragment, onRecognized: (String) -> Unit): PackageMatcher
    }

    private val context get() = fragment.requireContext()
    private val handledBlocks = mutableSetOf<String>()
    private var strugglingSinceMs: Long? = null

    // Set the instant one candidate block is picked up (confident match or ambiguous-needing-a-
    // dialog) and stays set through to a successful refill; only resumeScanning() (called by the
    // scan screen once the user taps "Next") clears it. A cancelled dialog also clears it early,
    // since nothing was decided and there's no reason to keep the camera frozen.
    private val sessionActive = AtomicBoolean(false)
    private val mutex = Mutex()

    /** Processes one frame's recognized text blocks. Safe to call repeatedly; never overlaps itself. */
    suspend fun handleBlocks(blocks: List<String>) {
        if (sessionActive.get()) return
        val normalized = blocks.map { normalize(it) }.filter { it.length >= MIN_BLOCK_LENGTH }
        if (normalized.isEmpty()) {
            strugglingSinceMs = null
            return
        }

        mutex.withLock {
            if (sessionActive.get()) return@withLock

            val medicines = medicineRepository.getAll()
            val labels = medicineLabelRepository.getAll()

            var bestUnmatchedCandidate: String? = null
            for (block in normalized) {
                if (block in handledBlocks) continue
                if (handleConfidentBlock(block, medicines, labels)) return@withLock
                if (block.length >= MIN_UNMATCHED_BLOCK_LENGTH &&
                    (bestUnmatchedCandidate == null || block.length > bestUnmatchedCandidate!!.length)
                ) {
                    bestUnmatchedCandidate = block
                }
            }

            val candidate = bestUnmatchedCandidate
            if (candidate == null || medicines.isEmpty()) {
                strugglingSinceMs = null
                return@withLock
            }
            val since = strugglingSinceMs ?: System.currentTimeMillis().also { strugglingSinceMs = it }
            if (System.currentTimeMillis() - since >= UNMATCHED_PATIENCE_MS) {
                strugglingSinceMs = null
                sessionActive.set(true)
                handledBlocks += candidate
                askWhichMedicine(candidate, medicines)
            }
        }
    }

    /** Called by the scan screen once the user taps "Next" after a successful recognition. */
    fun resumeScanning() {
        sessionActive.set(false)
        strugglingSinceMs = null
    }

    private fun cancelSession(block: String) {
        sessionActive.set(false)
        strugglingSinceMs = null
        handledBlocks.remove(block)
    }

    /** Returns true if [block] resolved to a confident or ambiguous-but-recognized package match. */
    private suspend fun handleConfidentBlock(block: String, medicines: List<Medicine>, labels: List<MedicineLabel>): Boolean {
        val blockKey = fuzzyKey(block)
        val labelMatch = labels.firstOrNull { blockKey.contains(fuzzyKey(it.text)) }
        if (labelMatch != null) {
            val medicine = medicines.firstOrNull { it.id == labelMatch.medicineId } ?: return false
            sessionActive.set(true)
            handledBlocks += block
            val quantity = labelMatch.quantity
            if (quantity != null) {
                refill(medicine, quantity)
            } else {
                askQuantity(medicine, block)
            }
            return true
        }

        val nameMatches = medicines.filter { it.name.isNotBlank() && blockKey.contains(fuzzyKey(it.name)) }
        return when (nameMatches.size) {
            0 -> false
            1 -> {
                val medicine = nameMatches[0]
                sessionActive.set(true)
                handledBlocks += block
                val quantity = QuantityParser.parse(block)
                if (quantity != null) {
                    medicineLabelRepository.remember(MedicineLabel(block, medicine.id, quantity))
                    refill(medicine, quantity)
                } else {
                    askQuantity(medicine, block)
                }
                true
            }

            else -> {
                sessionActive.set(true)
                handledBlocks += block
                askWhichMedicine(block, medicines)
                true
            }
        }
    }

    private suspend fun refill(medicine: Medicine, quantity: Double) {
        ReminderProcessorBroadcastReceiver.requestRefill(context, medicine.id, quantity)
        val message = context.getString(
            UiR.string.package_stock_refilled_amount,
            medicine.name,
            MedicineHelper.formatAmount(quantity, medicine.unit)
        )
        withContext(mainDispatcher) {
            onRecognized(message)
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
                    val quantity = input.text.toString().toDoubleOrNull()
                    if (quantity != null && quantity > 0) {
                        fragment.lifecycleScope.launch(dispatcher) {
                            medicineLabelRepository.remember(MedicineLabel(block, medicine.id, quantity))
                            refill(medicine, quantity)
                        }
                    } else {
                        cancelSession(block)
                    }
                }
                .setNegativeButton(UiR.string.cancel) { _, _ -> cancelSession(block) }
                .setOnCancelListener { cancelSession(block) }
                .show()
        }
    }

    private suspend fun askWhichMedicine(block: String, medicines: List<Medicine>) {
        withContext(mainDispatcher) {
            val names: Array<CharSequence> = medicines.map { it.name as CharSequence }.toTypedArray()
            MaterialAlertDialogBuilder(context)
                .setTitle(UiR.string.ocr_pick_medicine_title)
                .setItems(names) { _, which ->
                    val medicine = medicines[which]
                    fragment.lifecycleScope.launch(dispatcher) {
                        val quantity = QuantityParser.parse(block)
                        if (quantity != null) {
                            medicineLabelRepository.remember(MedicineLabel(block, medicine.id, quantity))
                            refill(medicine, quantity)
                        } else {
                            askQuantity(medicine, block)
                        }
                    }
                }
                .setNegativeButton(UiR.string.cancel) { _, _ -> cancelSession(block) }
                .setOnCancelListener { cancelSession(block) }
                .show()
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()

    private fun normalize(text: String): String = text.lowercase().replace(Regex("\\s+"), " ").trim()

    // Used only to compare a block against a medicine name/remembered label, never for
    // QuantityParser (which needs spaces to tell "500" from "500mg" from "n. 30"). Packaging text
    // and the app's own medicine names ("Depakin Chrono 500mg") rarely agree on whether there's a
    // space before a unit, where line breaks fall, or how punctuation is spaced - stripping
    // everything down to bare letters/digits makes the containment check tolerant of that noise
    // instead of requiring a byte-for-byte substring match.
    private fun fuzzyKey(text: String): String = text.filter { it.isLetterOrDigit() }

    companion object {
        private const val MIN_BLOCK_LENGTH = 4
        private const val MIN_UNMATCHED_BLOCK_LENGTH = 12
        private const val UNMATCHED_PATIENCE_MS = 2500L
    }
}
