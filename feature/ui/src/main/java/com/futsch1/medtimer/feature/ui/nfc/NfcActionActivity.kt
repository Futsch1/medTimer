package com.futsch1.medtimer.feature.ui.nfc

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.createCalendarEventIntent
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Entry point for the medtimer:// automation links: matched directly by Android's NFC dispatch
 * when a tag holds one of these as an NDEF URI record (see the intent filters in this module's
 * AndroidManifest.xml), so no Samsung-Routines-style middleman is needed - tapping the tag opens
 * this activity with the parsed URI on any phone.
 *
 * - take / refill: per-medicine links (?medicineId=N), copied from the medicine's stock settings.
 * - takeScheduled: one generic link, meant for a single tag used every day - marks every
 *   currently RAISED (not yet taken/skipped) dose reminder as taken, whatever is due right now.
 * - requestPrescription: fired from the out-of-stock notification's action button, not normally
 *   written to a tag by hand.
 *
 * Deliberately NOT also registered for a plain ACTION_VIEW/BROWSABLE: this activity is exported
 * (NFC dispatch requires it) and performs state-changing actions with no confirmation prompt, so
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
    lateinit var preferencesDataSource: PreferencesDataSource

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
        if (uri == null) {
            toast(getString(com.futsch1.medtimer.core.ui.R.string.nfc_action_invalid_link))
            finish()
            return
        }

        lifecycleScope.launch(dispatcher) {
            when (uri.host) {
                "take" -> withMedicineId(uri) { takeDose(it) }
                "refill" -> withMedicineId(uri) { refill(it) }
                "requestPrescription" -> withMedicineId(uri) { requestPrescription(it) }
                "takeScheduled" -> takeAllScheduled()
                else -> withContext(mainDispatcher) {
                    toast(getString(com.futsch1.medtimer.core.ui.R.string.nfc_action_invalid_link))
                }
            }
            withContext(mainDispatcher) { finish() }
        }
    }

    private suspend fun withMedicineId(uri: Uri, action: suspend (Int) -> Unit) {
        val medicineId = uri.getQueryParameter("medicineId")?.toIntOrNull()
        if (medicineId == null) {
            withContext(mainDispatcher) {
                toast(getString(com.futsch1.medtimer.core.ui.R.string.nfc_action_invalid_link))
            }
            return
        }
        action(medicineId)
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

    /** Marks every currently-RAISED dose reminder (across all medicines) as taken - excludes
     * out-of-stock/expiration/refill events, which aren't "doses" and use a different action. */
    private suspend fun takeAllScheduled() {
        val dueEvents = reminderEventRepository.getAllWithoutDeletedAndAcknowledged().filter {
            it.status == ReminderEvent.ReminderStatus.RAISED &&
                it.reminderType !in NON_DOSE_REMINDER_TYPES
        }
        if (dueEvents.isEmpty()) {
            withContext(mainDispatcher) {
                toast(getString(com.futsch1.medtimer.core.ui.R.string.nfc_action_nothing_due))
            }
            return
        }
        // Passing a null Reminder makes requestReminderAction skip the variableAmount prompt and
        // just mark taken with the event's already-stored amount - the same shape ManualDose and
        // the plain "Taken" button in the Overview list already use.
        for (event in dueEvents) {
            ReminderProcessorBroadcastReceiver.requestReminderAction(applicationContext, null, event, true)
        }
        withContext(mainDispatcher) {
            toast(getString(com.futsch1.medtimer.core.ui.R.string.nfc_action_batch_taken, dueEvents.size))
        }
    }

    /** Combines every currently out-of-stock medicine sharing this one's prescription contact
     * into a single SMS draft, and schedules a pickup reminder a configurable number of days out. */
    private suspend fun requestPrescription(medicineId: Int) {
        val medicine = medicineRepository.fetch(medicineId) ?: run {
            withContext(mainDispatcher) {
                toast(getString(com.futsch1.medtimer.core.ui.R.string.nfc_action_unknown_medicine))
            }
            return
        }
        val contact = medicine.prescriptionContact
        if (contact.isBlank()) return

        val lowStockNames = medicineRepository.getAll()
            .filter { it.isOutOfStock() && it.prescriptionContact == contact }
            .map(Medicine::name)
            .ifEmpty { listOf(medicine.name) }

        val smsBody = if (lowStockNames.size == 1) {
            getString(com.futsch1.medtimer.core.ui.R.string.prescription_request_sms, lowStockNames[0])
        } else {
            getString(
                com.futsch1.medtimer.core.ui.R.string.prescription_request_sms_multi,
                lowStockNames.joinToString(", ")
            )
        }
        val pickupDays = preferencesDataSource.preferences.value.prescriptionPickupDays
        val pickupDate = LocalDate.now().plusDays(pickupDays.toLong())

        withContext(mainDispatcher) {
            safeStartActivity(
                createCalendarEventIntent(
                    getString(com.futsch1.medtimer.core.ui.R.string.prescription_pickup_event_title), pickupDate
                )
            )
            safeStartActivity(
                Intent(Intent.ACTION_SENDTO, "smsto:$contact".toUri()).putExtra("sms_body", smsBody)
            )
        }
    }

    private fun safeStartActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No app to handle it (no calendar/SMS app) - nothing sensible to do here, the other
            // intent (if any) still gets its own attempt.
        }
    }

    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val NON_DOSE_REMINDER_TYPES = setOf(
            ReminderType.OUT_OF_STOCK, ReminderType.EXPIRATION_DATE, ReminderType.REFILL
        )

        fun buildTakeUri(medicineId: Int): Uri =
            Uri.parse("medtimer://take").buildUpon().appendQueryParameter("medicineId", medicineId.toString()).build()

        fun buildRefillUri(medicineId: Int): Uri =
            Uri.parse("medtimer://refill").buildUpon().appendQueryParameter("medicineId", medicineId.toString()).build()

        fun buildTakeScheduledUri(): Uri = Uri.parse("medtimer://takeScheduled")
    }
}
