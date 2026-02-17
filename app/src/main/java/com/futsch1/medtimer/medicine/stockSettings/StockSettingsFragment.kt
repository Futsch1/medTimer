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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.EntityDataStore
import com.futsch1.medtimer.helpers.EntityPreferencesFragment
import com.futsch1.medtimer.helpers.EntityViewModel
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.createCalendarEventIntent
import com.futsch1.medtimer.medicine.advancedReminderPreferences.showDateEdit
import com.futsch1.medtimer.medicine.dialogs.NewReminderStockDialog
import com.futsch1.medtimer.medicine.estimateStockRunOutDate
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver
import kotlinx.coroutines.launch
import java.text.DecimalFormatSymbols

class StockSettingsFragment(
) : EntityPreferencesFragment<FullMedicine>(
    R.xml.stock_settings,
    mapOf(
    ),
    mapOf(
    ),
    listOf("stock_unit")
) {
    override val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>
        get() = mapOf(
            "stock_run_out_to_calendar" to { _, _ -> addToCalendar() },
            "stock_refill_now" to { _, _ -> refillNow() },
            "production_date" to { activity, preference -> showDateEdit(activity, preference) },
            "expiration_date" to { activity, preference -> showDateEdit(activity, preference) },
            "clear_dates" to { _, _ ->
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

    override fun getEntityDataStore(requireArguments: Bundle): EntityDataStore<FullMedicine> {
        return MedicineDataStore(requireArguments.getInt("medicineId"), requireContext(), getEntityViewModel().medicineRepository)
    }

    override fun getEntityViewModel(): EntityViewModel<FullMedicine> {
        return ViewModelProvider(this)[StockMedicineViewModel::class.java]
    }

    override fun customSetup(entity: FullMedicine) {
        setupAmountEdit(findPreference("amount")!!)
        setupAmountEdit(findPreference("stock_refill_size")!!)

        calculateRunOutDate(entity)
    }

    override fun onEntityUpdated(entity: FullMedicine) {
        super.onEntityUpdated(entity)

        calculateRunOutDate(entity)

        findPreference<EditTextPreference>("amount")!!.summary = MedicineHelper.formatAmount(entity.medicine.amount, entity.medicine.unit)
        findPreference<EditTextPreference>("stock_refill_size")!!.summary = MedicineHelper.formatAmount(entity.medicine.refillSize, entity.medicine.unit)
        if (entity.isOutOfStock) {
            findPreference<EditTextPreference>("amount")!!.setIcon(R.drawable.exclamation_triangle_fill)
        } else {
            findPreference<EditTextPreference>("amount")!!.icon = null
        }
        if (entity.medicine.hasExpired()) {
            findPreference<Preference>("expiration_date")!!.setIcon(R.drawable.ban)
        } else {
            findPreference<Preference>("expiration_date")!!.icon = null
        }
        findPreference<Preference>("production_date")!!.summary = if (entity.medicine.productionDate != 0L) {
            TimeHelper.daysSinceEpochToDateString(context, entity.medicine.productionDate)
        } else {
            ""
        }
        findPreference<Preference>("expiration_date")!!.summary = if (entity.medicine.expirationDate != 0L) {
            TimeHelper.daysSinceEpochToDateString(context, entity.medicine.expirationDate)
        } else {
            ""
        }
    }

    private fun calculateRunOutDate(entity: FullMedicine) {
        val viewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
        this.lifecycleScope.launch(ioDispatcher) {
            val runOutDate = estimateStockRunOutDate(viewModel, entity.medicine.medicineId, entity.medicine.amount)

            val runOutString = if (runOutDate != null && context != null) TimeHelper.localDateToString(context, runOutDate) else "---"

            this.launch(mainDispatcher) {
                findPreference<EditTextPreference>("stock_run_out_date")!!.summary = runOutString
            }
        }
    }

    private fun addToCalendar() {
        val date = TimeHelper.stringToLocalDate(context, findPreference<EditTextPreference>("stock_run_out_date")!!.summary.toString())
        if (date != null) {
            val intent = createCalendarEventIntent(context?.getString(R.string.out_of_stock_notification_title) + " - " + dataStore.entity.medicine.name, date)
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.no_calendar_app, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refillNow() {
        ReminderProcessorBroadcastReceiver.requestRefill(requireContext(), dataStore.entity.medicine.medicineId)
    }

    private fun showCreateNewReminderStockDialog(activity: FragmentActivity) {
        val reminder = Reminder(dataStore.entity.medicine.medicineId)
        reminder.outOfStockThreshold = if (dataStore.entity.medicine.amount > 0.0) dataStore.entity.medicine.amount else 1.0
        reminder.outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE
        NewReminderStockDialog(activity, dataStore.entity.medicine, medicineRepository, reminder)
    }

    private fun showCreateNewReminderExpirationDateDialog(activity: FragmentActivity) {
        val reminder = Reminder(dataStore.entity.medicine.medicineId)
        reminder.expirationReminderType = Reminder.ExpirationReminderType.ONCE
        NewReminderStockDialog(activity, dataStore.entity.medicine, medicineRepository, reminder)
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

