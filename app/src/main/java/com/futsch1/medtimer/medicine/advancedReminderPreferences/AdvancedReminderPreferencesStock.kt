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
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.medicine.stockSettings.setupAmountEdit
import kotlinx.coroutines.launch

class AdvancedReminderPreferencesStockFragment(
) : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_stock,
    mapOf(
    ),
    mapOf(
    ),
    listOf("stock_threshold", "expiration_days_before")
) {
    val menuProvider = AdvancedReminderSettingsMenuProvider(this)
    var fullMedicine: FullMedicine? = null

    override val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>
        get() = mapOf("edit_medicine_stock_settings" to { _, _ ->
            findNavController().navigate(
                AdvancedReminderPreferencesStockFragmentDirections.actionAdvancedReminderPreferencesStockExpirationFragmentToStockSettingsFragment(
                    dataStore.entity.medicineRelId
                )
            )
        })

    override fun onEntityUpdated(entity: Reminder) {
        super.onEntityUpdated(entity)

        menuProvider.medicineRepository = medicineRepository
        menuProvider.reminder = entity
    }

    override fun customSetup(entity: Reminder) {
        setupAmountEdit(findPreference("stock_threshold")!!)

        findPreference<Preference>("stock_threshold")?.isVisible = entity.reminderType == Reminder.ReminderType.OUT_OF_STOCK
        findPreference<Preference>("stock_reminder")?.isVisible = entity.reminderType == Reminder.ReminderType.OUT_OF_STOCK
        findPreference<Preference>("medicine_stock")?.isVisible = entity.reminderType == Reminder.ReminderType.OUT_OF_STOCK
        findPreference<Preference>("expiration_reminder")?.isVisible = entity.reminderType == Reminder.ReminderType.EXPIRATION_DATE
        findPreference<Preference>("expiration_days_before")?.isVisible = entity.reminderType == Reminder.ReminderType.EXPIRATION_DATE
        findPreference<Preference>("medicine_expiration_date")?.isVisible = entity.reminderType == Reminder.ReminderType.EXPIRATION_DATE

        this.lifecycleScope.launch(ioDispatcher) {
            fullMedicine = medicineRepository.getMedicine(entity.medicineRelId)
            this.launch(mainDispatcher) {
                if (fullMedicine != null) {
                    findPreference<Preference>("medicine_stock")?.summary =
                        MedicineHelper.formatAmount(fullMedicine!!.medicine.amount, fullMedicine!!.medicine.unit)
                    findPreference<Preference>("medicine_expiration_date")?.summary = if (fullMedicine!!.medicine.expirationDate != 0L) {

                        TimeHelper.daysSinceEpochToDateString(context, fullMedicine!!.medicine.expirationDate)
                    } else {
                        context?.getString(R.string.never)
                    }

                }
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