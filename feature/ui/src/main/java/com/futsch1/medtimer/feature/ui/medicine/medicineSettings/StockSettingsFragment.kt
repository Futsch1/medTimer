package com.futsch1.medtimer.feature.ui.medicine.medicineSettings

import android.content.ActivityNotFoundException
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.createCalendarEventIntent
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.FutureRemindersRepository
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver
import com.futsch1.medtimer.feature.ui.R
import com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences.DateEditHandler
import com.futsch1.medtimer.feature.ui.medicine.dialogs.NewReminderStockDialog
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
    lateinit var futureRemindersRepository: FutureRemindersRepository

    @Inject
    lateinit var newReminderStockDialogFactory: NewReminderStockDialog.Factory

    @Inject
    lateinit var dateEditHandler: DateEditHandler

    @Inject
    lateinit var timeFormatter: TimeFormatter

    override val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>
        get() = mapOf(
            "stock_run_out_to_calendar" to { _, _ -> addToCalendar() },
            "stock_refill_now" to { _, _ -> refillNow() },
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
        futureRemindersRepository.requestWindow("stockSettings", 365)
    }

    override fun onStop() {
        super.onStop()
        futureRemindersRepository.releaseWindow("stockSettings")
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
    }

    private fun observeRunOutDate(medicineId: Int) {
        lifecycleScope.launch {
            futureRemindersRepository.stockRunOutDates
                .collect { dates ->
                    val runOutDate = dates[medicineId]
                    val simulatedThrough = futureRemindersRepository.simulatedThrough.value
                    val summary = when {
                        runOutDate != null -> timeFormatter.localDateToString(runOutDate)
                        simulatedThrough == LocalDate.MIN -> "---"
                        else -> context?.getString(
                            com.futsch1.medtimer.core.ui.R.string.stock_after_simulation_end,
                            timeFormatter.localDateToString(simulatedThrough)
                        ) ?: "---"
                    }
                    withContext(mainDispatcher) {
                        findPreference<EditTextPreference>("stock_run_out_date")?.summary = summary
                    }
                }
        }
    }

    private fun addToCalendar() {
        val date = futureRemindersRepository.stockRunOutDates.value[dataStore.modelData.id]
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
