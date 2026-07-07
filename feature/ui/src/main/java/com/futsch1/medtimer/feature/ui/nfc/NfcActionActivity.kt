package com.futsch1.medtimer.feature.ui.nfc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/**
 * Entry point for medtimer://take?medicineId=N and medtimer://refill?medicineId=N: matched
 * directly by Android's NFC dispatch when a tag holds one of these as an NDEF URI record (see
 * the intent filters in this module's AndroidManifest.xml), so no Samsung-Routines-style
 * middleman is needed - tapping the tag opens this activity with the parsed URI on any phone.
 *
 * Deliberately NOT also registered for a plain ACTION_VIEW/BROWSABLE: this activity is exported
 * (NFC dispatch requires it) and performs a state-changing action with no confirmation prompt, so
 * it shouldn't also answer to a scheme any other installed app or web page could construct and
 * fire directly. NDEF_DISCOVERED is inherently exposed the same way in principle, but at least
 * narrows it to the one entry point this feature actually needs.
 *
 * No visible UI: performs the action and finishes, with a Toast as the only feedback.
 */
@AndroidEntryPoint
class NfcActionActivity : AppCompatActivity() {

    @Inject
    lateinit var medicineRepository: MedicineRepository

    @Inject
    lateinit var reminderEventRepository: ReminderEventRepository

    @Inject
    @Dispatcher(MedTimerDispatchers.IO)
    lateinit var dispatcher: CoroutineDispatcher

    @Inject
    @Dispatcher(MedTimerDispatchers.Main)
    lateinit var mainDispatcher: CoroutineDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        val uri = intent.data
        val medicineId = uri?.getQueryParameter("medicineId")?.toIntOrNull()
        if (uri == null || medicineId == null) {
            toast(getString(com.futsch1.medtimer.core.ui.R.string.nfc_action_invalid_link))
            finish()
            return
        }

        lifecycleScope.launch(dispatcher) {
            when (uri.host) {
                "take" -> takeDose(medicineId)
                "refill" -> refill(medicineId)
                else -> withContext(mainDispatcher) {
                    toast(getString(com.futsch1.medtimer.core.ui.R.string.nfc_action_invalid_link))
                }
            }
            withContext(mainDispatcher) { finish() }
        }
    }

    private suspend fun takeDose(medicineId: Int) {
        val medicine = medicineRepository.fetch(medicineId) ?: run {
            withContext(mainDispatcher) {
                toast(getString(com.futsch1.medtimer.core.ui.R.string.nfc_action_unknown_medicine))
            }
            return
        }
        val now = Instant.now()
        // No specific reminder context to draw a dose amount from - fall back to the medicine's
        // first configured reminder amount, same default used for reminder-less manual dose entries.
        val amount = medicine.reminders.firstOrNull()?.amount.orEmpty()

        reminderEventRepository.create(
            ReminderEvent.default().copy(
                reminderId = -1,
                medicineName = medicine.name,
                color = medicine.color,
                useColor = medicine.useColor,
                iconId = medicine.iconId,
                tags = medicine.tags.map { it.name },
                amount = amount,
                status = ReminderEvent.ReminderStatus.TAKEN,
                remindedTimestamp = now,
                processedTimestamp = now
            )
        )
        MedicineHelper.parseAmount(amount)?.let { parsedAmount ->
            ReminderProcessorBroadcastReceiver.requestStockHandling(
                applicationContext, parsedAmount, medicineId, now.epochSecond
            )
        }
        withContext(mainDispatcher) {
            toast(getString(com.futsch1.medtimer.core.ui.R.string.nfc_action_dose_logged, medicine.name))
        }
    }

    private suspend fun refill(medicineId: Int) {
        val medicine = medicineRepository.fetch(medicineId) ?: run {
            withContext(mainDispatcher) {
                toast(getString(com.futsch1.medtimer.core.ui.R.string.nfc_action_unknown_medicine))
            }
            return
        }
        ReminderProcessorBroadcastReceiver.requestRefill(applicationContext, medicineId)
        withContext(mainDispatcher) {
            toast(getString(com.futsch1.medtimer.core.ui.R.string.package_stock_refilled, medicine.name))
        }
    }

    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun buildTakeUri(medicineId: Int): Uri =
            Uri.parse("medtimer://take").buildUpon().appendQueryParameter("medicineId", medicineId.toString()).build()

        fun buildRefillUri(medicineId: Int): Uri =
            Uri.parse("medtimer://refill").buildUpon().appendQueryParameter("medicineId", medicineId.toString()).build()
    }
}
