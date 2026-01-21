package com.futsch1.medtimer.medicine.stockSettings

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.helpers.EntityDataStore
import com.futsch1.medtimer.helpers.EntityPreferencesFragment
import com.futsch1.medtimer.helpers.EntityViewModel
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.createCalendarEventIntent
import com.futsch1.medtimer.medicine.addDoubleValidator
import com.futsch1.medtimer.medicine.estimateStockRunOutDate
import kotlinx.coroutines.launch
import java.text.DecimalFormatSymbols

class StockSettingsFragment(
) : EntityPreferencesFragment<Medicine>(
    R.xml.stock_settings,
    mapOf(
    ),
    mapOf(
    ),
    listOf("stock_unit")
) {
    override val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>
        get() = mapOf(
            "stock_run_out_to_calendar" to { _, _ -> addToCalendar() }
        )

    override fun getEntityDataStore(requireArguments: Bundle): EntityDataStore<Medicine> {
        return MedicineDataStore(requireArguments.getInt("medicineId"), requireContext(), getEntityViewModel().medicineRepository)
    }

    override fun getEntityViewModel(): EntityViewModel<Medicine> {
        return ViewModelProvider(this)[StockMedicineViewModel::class.java]
    }

    override fun customSetup(entity: Medicine) {
        setupAmountEdit("amount")
        setupAmountEdit("stock_threshold")
        setupAmountEdit("stock_refill_size")

        calculateRunOutDate(entity)
    }

    override fun onEntityUpdated(entity: Medicine) {
        super.onEntityUpdated(entity)

        calculateRunOutDate(entity)

        findPreference<EditTextPreference>("amount")!!.summary = MedicineHelper.formatAmount(entity.amount, entity.unit)
    }

    private fun calculateRunOutDate(entity: Medicine) {
        val viewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
        this.lifecycleScope.launch(ioDispatcher) {
            val runOutDate = estimateStockRunOutDate(viewModel, entity.medicineId, entity.amount)

            val runOutString = if (runOutDate != null && context != null) TimeHelper.localDateToString(context, runOutDate) else "---"

            this.launch(mainDispatcher) {
                findPreference<EditTextPreference>("stock_run_out_date")!!.summary = runOutString
            }
        }
    }


    private fun setupAmountEdit(editName: String) {
        findPreference<EditTextPreference>(editName)!!.setOnBindEditTextListener { editText ->
            val separator = DecimalFormatSymbols.getInstance().decimalSeparator
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789$separator"))
            editText.addDoubleValidator()
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
    }

    private fun addToCalendar() {
        val date = TimeHelper.stringToLocalDate(context, findPreference<EditTextPreference>("stock_run_out_date")!!.summary.toString())
        if (date != null) {
            val intent = createCalendarEventIntent(context?.getString(R.string.out_of_stock_notification_title) + " - " + dataStore.entity.name, date)
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.no_calendar_app, Toast.LENGTH_SHORT).show()
            }
        }
    }
}