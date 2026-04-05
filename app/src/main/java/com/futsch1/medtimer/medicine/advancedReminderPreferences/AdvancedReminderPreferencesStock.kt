package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.medicine.stockSettings.setupAmountEdit
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class AdvancedReminderPreferencesStockFragment : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_stock,
    mapOf(
    ),
    mapOf(
    ),
    listOf("stock_threshold", "expiration_days_before")
) {
    @Inject
    lateinit var medicineRepository: MedicineRepository

    @Inject
    lateinit var timeFormatter: TimeFormatter

    @Inject
    lateinit var menuProviderFactory: AdvancedReminderSettingsMenuProvider.Factory

    val menuProvider by lazy { menuProviderFactory.create(this) }
    var medicine: Medicine? = null

    override val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>
        get() = mapOf("edit_medicine_stock_settings" to { _, _ ->
            findNavController().navigate(
                AdvancedReminderPreferencesStockFragmentDirections.actionAdvancedReminderPreferencesStockExpirationFragmentToStockSettingsFragment(
                    dataStore.modelData.medicineRelId
                )
            )
        })

    override fun onModelDataUpdated(modelData: Reminder) {
        super.onModelDataUpdated(modelData)

        menuProvider.reminder = modelData

        findPreference<Preference>("stock_threshold")?.summary = MedicineHelper.formatAmount(modelData.outOfStockThreshold, medicine?.unit ?: "")
    }

    override fun customSetup(modelData: Reminder) {
        setupAmountEdit(findPreference("stock_threshold")!!)

        findPreference<Preference>("stock_threshold")?.isVisible = modelData.reminderType == ReminderType.OUT_OF_STOCK
        findPreference<Preference>("stock_reminder")?.isVisible = modelData.reminderType == ReminderType.OUT_OF_STOCK
        findPreference<Preference>("medicine_stock")?.isVisible = modelData.reminderType == ReminderType.OUT_OF_STOCK
        findPreference<Preference>("expiration_reminder")?.isVisible = modelData.reminderType == ReminderType.EXPIRATION_DATE
        findPreference<Preference>("expiration_days_before")?.isVisible = modelData.reminderType == ReminderType.EXPIRATION_DATE
        findPreference<Preference>("medicine_expiration_date")?.isVisible = modelData.reminderType == ReminderType.EXPIRATION_DATE

        this.lifecycleScope.launch(ioDispatcher) {
            val medicine = medicineRepository.get(modelData.medicineRelId) ?: return@launch
            this.launch(mainDispatcher) {
                findPreference<Preference>("medicine_stock")?.summary =
                    MedicineHelper.formatAmount(medicine.amount, medicine.unit)
                findPreference<Preference>("medicine_expiration_date")?.summary = if (medicine.expirationDate != LocalDate.EPOCH) {
                    timeFormatter.localDateToString(medicine.expirationDate)
                } else {
                    context?.getString(R.string.never)
                }
                this@AdvancedReminderPreferencesStockFragment.medicine = medicine
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        requireActivity().addMenuProvider(
            menuProvider,
            getViewLifecycleOwner()
        )

        return view
    }

}