package com.futsch1.medtimer.medicine.stockSettings

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.ModelDataPreferencesFragment
import com.futsch1.medtimer.helpers.ModelDataStore
import com.futsch1.medtimer.helpers.ModelDataViewModel
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.helpers.createCalendarEventIntent
import com.futsch1.medtimer.medicine.advancedReminderPreferences.DateEditHandler
import com.futsch1.medtimer.medicine.dialogs.NewReminderStockDialog
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver
import com.futsch1.medtimer.reminders.SystemTimeAccess
import com.futsch1.medtimer.reminders.scheduling.SchedulingSimulator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class StockSettingsFragment : ModelDataPreferencesFragment<Medicine>(
    R.xml.stock_settings,
    mapOf(
    ),
    mapOf(
    ),
    listOf("stock_unit")
) {
    @Inject
    lateinit var medicineRepository: MedicineRepository

    @Inject
    lateinit var reminderEventRepository: ReminderEventRepository

    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    @Inject
    lateinit var newReminderStockDialogFactory: NewReminderStockDialog.Factory

    @Inject
    lateinit var dateEditHandler: DateEditHandler

    @Inject
    lateinit var timeFormatter: TimeFormatter

    @Inject
    lateinit var medicineDataStoreFactory: MedicineDataStore.Factory

    @Inject
    lateinit var timeAccess: SystemTimeAccess

    private val stockMedicineViewModel: StockMedicineViewModel by viewModels()

    override val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>
        get() = mapOf(
            "stock_run_out_to_calendar" to { _, _ -> addToCalendar() },
            "stock_refill_now" to { _, _ -> refillNow() },
            "production_date" to { activity, preference -> dateEditHandler.show(activity, preference) },
            "expiration_date" to { activity, preference -> dateEditHandler.show(activity, preference) },
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

    override suspend fun getDataStore(requireArguments: Bundle): ModelDataStore<Medicine> {
        val entityId = requireArguments.getInt("medicineId")
        val entity = medicineRepository.get(entityId)!!
        return medicineDataStoreFactory.create(entity)
    }

    override fun getEntityViewModel(): ModelDataViewModel<Medicine> = stockMedicineViewModel

    override fun customSetup(modelData: Medicine) {
        setupAmountEdit(findPreference("amount")!!)
        setupAmountEdit(findPreference("stock_refill_size")!!)

        calculateRunOutDate(modelData)
    }

    override fun onModelDataUpdated(modelData: Medicine) {
        super.onModelDataUpdated(modelData)

        calculateRunOutDate(modelData)

        findPreference<EditTextPreference>("amount")!!.summary = MedicineHelper.formatAmount(modelData.amount, modelData.unit)
        findPreference<EditTextPreference>("stock_refill_size")!!.summary = MedicineHelper.formatAmount(modelData.refillSize, modelData.unit)
        if (modelData.isOutOfStock()) {
            findPreference<EditTextPreference>("amount")!!.setIcon(R.drawable.exclamation_triangle_fill)
        } else {
            findPreference<EditTextPreference>("amount")!!.icon = null
        }
        if (modelData.hasExpired()) {
            findPreference<Preference>("expiration_date")!!.setIcon(R.drawable.ban)
        } else {
            findPreference<Preference>("expiration_date")!!.icon = null
        }
        findPreference<Preference>("production_date")!!.summary = if (modelData.productionDate != LocalDate.EPOCH) {
            timeFormatter.localDateToString(modelData.productionDate)
        } else {
            ""
        }
        findPreference<Preference>("expiration_date")!!.summary = if (modelData.expirationDate != LocalDate.EPOCH) {
            timeFormatter.localDateToString(modelData.expirationDate)
        } else {
            ""
        }
    }

    private fun calculateRunOutDate(medicine: Medicine) {
        this.lifecycleScope.launch(ioDispatcher) {
            val runOutDate = estimateStockRunOutDate(medicine.id, medicine.amount)

            val runOutString = if (runOutDate != null && context != null) timeFormatter.localDateToString(runOutDate) else "---"

            withContext(mainDispatcher) {
                findPreference<EditTextPreference>("stock_run_out_date")!!.summary = runOutString
            }
        }
    }

    private suspend fun estimateStockRunOutDate(
        medicineId: Int,
        currentAmount: Double? = null
    ): LocalDate? {
        var medicine = medicineRepository.get(medicineId) ?: return null

        if (currentAmount != null) {
            medicine = medicine.copy(amount = currentAmount)
        }
        val recentReminders =
            reminderEventRepository.getForScheduling(listOf(medicine)).filter { it.status != ReminderEvent.ReminderStatus.RAISED }
        val schedulingSimulator = SchedulingSimulator(listOf(medicine), recentReminders, timeAccess, preferencesDataSource)
        val endDate = LocalDate.now().plusDays(365 * 2)
        var runOutDate: LocalDate? = null

        schedulingSimulator.simulate { _, scheduledDate: LocalDate, amount: Double ->
            if (amount == 0.0) {
                runOutDate = scheduledDate
            }
            scheduledDate <= endDate && amount != 0.0
        }

        return runOutDate
    }

    private fun addToCalendar() {
        val date = timeFormatter.stringToLocalDate(findPreference<EditTextPreference>("stock_run_out_date")!!.summary.toString())
        if (date != null) {
            val intent =
                createCalendarEventIntent("\uD83D\uDC8A " + context?.getString(R.string.out_of_stock_notification_title) + " - " + dataStore.modelData.name, date)
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.no_calendar_app, Toast.LENGTH_SHORT).show()
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
        val separator = DecimalFormatSymbols.getInstance().decimalSeparator
        editText.setKeyListener(DigitsKeyListener.getInstance("0123456789$separator"))
        editText.addDoubleValidator()
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }
}

