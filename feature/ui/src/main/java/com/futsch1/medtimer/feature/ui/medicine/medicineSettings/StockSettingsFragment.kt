package com.futsch1.medtimer.feature.ui.medicine.medicineSettings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.createCalendarEventIntent
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.ui.MedicineStringFormatter
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.SimulatedRemindersRepository
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver
import com.futsch1.medtimer.feature.ui.R
import com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences.DateEditHandler
import com.futsch1.medtimer.feature.ui.medicine.dialogs.NewReminderStockDialog
import com.futsch1.medtimer.feature.ui.nfc.NfcActionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class StockSettingsFragment : MedicinePreferences(
    R.xml.stock_settings,
    mapOf(
    ),
    mapOf(
    ),
    listOf("stock_unit")
) {
    @Inject
    lateinit var simulatedRemindersRepository: SimulatedRemindersRepository

    @Inject
    lateinit var newReminderStockDialogFactory: NewReminderStockDialog.Factory

    @Inject
    lateinit var dateEditHandler: DateEditHandler

    @Inject
    lateinit var timeFormatter: TimeFormatter

    @Inject
    lateinit var medicineStringFormatter: MedicineStringFormatter

    override val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>
        get() = mapOf(
            "stock_run_out_to_calendar" to { _, _ -> addToCalendar() },
            "stock_refill_now" to { _, _ -> refillNow() },
            "nfc_take_link" to { _, _ -> copyLinkToClipboard(NfcActionActivity.buildTakeUri(dataStore.modelData.id)) },
            "nfc_refill_link" to { _, _ -> copyLinkToClipboard(NfcActionActivity.buildRefillUri(dataStore.modelData.id)) },
            "production_date" to { activity, preference ->
                dateEditHandler.show(
                    activity,
                    preference
                )
            },
            "expiration_date" to { activity, preference ->
                dateEditHandler.show(
                    activity,
                    preference
                )
            },
            "clear_dates" to { _, _ ->
                // TODO: direct store usage in UI code; all non-UI logic should be delegated to the viewmodel
                dataStore.putLong("production_date", 0)
                dataStore.putLong("expiration_date", 0)
            },
            "create_out_of_stock_reminder" to { activity, _ ->
                showCreateNewReminderStockDialog(activity)
            },
            "create_expiration_date_reminder" to { activity, _ ->
                showCreateNewReminderExpirationDateDialog(activity)
            }
        )

    override fun onStart() {
        super.onStart()
        simulatedRemindersRepository.requestWindow("stockSettings", 365)
    }

    override fun onStop() {
        super.onStop()
        simulatedRemindersRepository.releaseWindow("stockSettings")
    }

    override fun customSetup(modelData: Medicine) {
        setupAmountEdit(findPreference("amount")!!)
        setupAmountEdit(findPreference("stock_refill_size")!!)

        observeRunOutDate(modelData.id)
    }

    override fun onModelDataUpdated(modelData: Medicine) {
        super.onModelDataUpdated(modelData)

        findPreference<EditTextPreference>("amount")!!.summary =
            MedicineHelper.formatAmount(modelData.amount, modelData.unit)
        findPreference<EditTextPreference>("stock_refill_size")!!.summary =
            MedicineHelper.formatAmount(modelData.refillSize, modelData.unit)
        if (modelData.isOutOfStock()) {
            findPreference<EditTextPreference>("amount")!!.setIcon(com.futsch1.medtimer.core.ui.R.drawable.exclamation_triangle_fill)
        } else {
            findPreference<EditTextPreference>("amount")!!.icon = null
        }
        if (modelData.hasExpired()) {
            findPreference<Preference>("expiration_date")!!.setIcon(com.futsch1.medtimer.core.ui.R.drawable.ban)
        } else {
            findPreference<Preference>("expiration_date")!!.icon = null
        }
        findPreference<Preference>("production_date")!!.summary =
            if (modelData.productionDate != LocalDate.EPOCH) {
                timeFormatter.localDateToString(modelData.productionDate)
            } else {
                ""
            }
        findPreference<Preference>("expiration_date")!!.summary =
            if (modelData.expirationDate != LocalDate.EPOCH) {
                timeFormatter.localDateToString(modelData.expirationDate)
            } else {
                ""
            }
        findPreference<Preference>("nfc_take_link")!!.summary = NfcActionActivity.buildTakeUri(modelData.id).toString()
        findPreference<Preference>("nfc_refill_link")!!.summary = NfcActionActivity.buildRefillUri(modelData.id).toString()
    }

    private fun copyLinkToClipboard(uri: Uri) {
        val clipboardManager = requireContext().getSystemService<ClipboardManager>() ?: return
        clipboardManager.setPrimaryClip(ClipData.newPlainText(uri.toString(), uri.toString()))
        Toast.makeText(context, com.futsch1.medtimer.core.ui.R.string.nfc_link_copied, Toast.LENGTH_SHORT).show()
    }

    private fun observeRunOutDate(medicineId: Int) {
        lifecycleScope.launch {
            simulatedRemindersRepository.stockRunOutDates
                .collect { dates ->
                    val runOutDate = dates[medicineId]
                    val simulatedThrough = simulatedRemindersRepository.simulatedThrough.value
                    val summary = medicineStringFormatter.getStockRunOutText(runOutDate, simulatedThrough)
                    withContext(mainDispatcher) {
                        findPreference<EditTextPreference>("stock_run_out_date")?.summary = summary

                        if (runOutDate == null) {
                            findPreference<Preference>("stock_run_out_to_calendar")?.isVisible = false
                            findPreference<Preference>("create_out_of_stock_reminder")?.isVisible = false
                        } else {
                            findPreference<Preference>("stock_run_out_to_calendar")?.isVisible = true
                            findPreference<Preference>("create_out_of_stock_reminder")?.isVisible = true
                        }
                    }
                }
        }
    }

    private fun addToCalendar() {
        val date = simulatedRemindersRepository.stockRunOutDates.value[dataStore.modelData.id]
        if (date != null) {
            val intent =
                createCalendarEventIntent(
                    "💊 " + context?.getString(com.futsch1.medtimer.core.ui.R.string.out_of_stock_notification_title) + " - " + dataStore.modelData.name,
                    date
                )
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    com.futsch1.medtimer.core.ui.R.string.no_calendar_app,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun refillNow() {
        ReminderProcessorBroadcastReceiver.requestRefill(requireContext(), dataStore.modelData.id)
    }

    private fun showCreateNewReminderStockDialog(activity: FragmentActivity) {
        val reminder = Reminder.default().copy(
            medicineRelId = dataStore.modelData.id,
            outOfStockThreshold = if (dataStore.modelData.amount > 0.0) dataStore.modelData.amount else 1.0,
            outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE
        )
        newReminderStockDialogFactory.create(activity, dataStore.modelData, reminder)
    }

    private fun showCreateNewReminderExpirationDateDialog(activity: FragmentActivity) {
        val reminder = Reminder.default().copy(
            medicineRelId = dataStore.modelData.id,
            expirationReminderType = Reminder.ExpirationReminderType.ONCE
        )
        newReminderStockDialogFactory.create(activity, dataStore.modelData, reminder)
    }
}

fun EditText.addDoubleValidator() {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Only afterTextChanged required
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // Only afterTextChanged required
        }

        override fun afterTextChanged(s: Editable?) {
            if (s == null || s.toString().isEmpty()) return

            val parsed: Double? = MedicineHelper.parseAmount(s.toString())
            if (parsed == null || parsed.isNaN() || parsed < 0.0) {
                // If the input is not a valid double, remove the last character
                s.delete(s.length - 1, s.length)
            }
        }
    })
}

fun setupAmountEdit(editTextPreference: EditTextPreference) {
    editTextPreference.setOnBindEditTextListener { editText ->
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        editText.addDoubleValidator()
        editText.keyListener = DigitsKeyListener.getInstance(Locale.getDefault(), false, true)
    }
}
