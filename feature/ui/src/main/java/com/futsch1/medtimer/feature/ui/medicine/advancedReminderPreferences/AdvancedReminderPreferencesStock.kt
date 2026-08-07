package com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.ui.R
import com.futsch1.medtimer.feature.ui.medicine.medicineSettings.setupAmountEdit
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
    lateinit var settingsActionsFactory: AdvancedReminderSettingsActions.Factory

    val settingsActions by lazy { settingsActionsFactory.create(this) }
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

        settingsActions.reminder = modelData

        findPreference<Preference>("stock_threshold")?.summary =
            MedicineHelper.formatAmount(modelData.outOfStockThreshold, medicine?.unit ?: "")
    }

    override fun customSetup(modelData: Reminder) {
        setupAmountEdit(findPreference("stock_threshold")!!)

        findPreference<Preference>("stock_threshold")?.isVisible =
            modelData.reminderType == ReminderType.OUT_OF_STOCK
        findPreference<Preference>("stock_reminder")?.isVisible =
            modelData.reminderType == ReminderType.OUT_OF_STOCK
        findPreference<Preference>("medicine_stock")?.isVisible =
            modelData.reminderType == ReminderType.OUT_OF_STOCK
        findPreference<Preference>("expiration_reminder")?.isVisible =
            modelData.reminderType == ReminderType.EXPIRATION_DATE
        findPreference<Preference>("expiration_days_before")?.isVisible =
            modelData.reminderType == ReminderType.EXPIRATION_DATE
        findPreference<Preference>("medicine_expiration_date")?.isVisible =
            modelData.reminderType == ReminderType.EXPIRATION_DATE

        this.lifecycleScope.launch(ioDispatcher) {
            val medicine = medicineRepository.fetch(modelData.medicineRelId) ?: return@launch
            this.launch(mainDispatcher) {
                findPreference<Preference>("medicine_stock")?.summary =
                    MedicineHelper.formatAmount(medicine.amount, medicine.unit)
                findPreference<Preference>("medicine_expiration_date")?.summary =
                    if (medicine.expirationDate != LocalDate.EPOCH) {
                        timeFormatter.localDateToString(medicine.expirationDate)
                    } else {
                        context?.getString(com.futsch1.medtimer.core.ui.R.string.never)
                    }
                this@AdvancedReminderPreferencesStockFragment.medicine = medicine
            }
        }
    }

    override val topAppBarActions: @Composable RowScope.() -> Unit =
        { AdvancedReminderSettingsMenu(settingsActions) }

}