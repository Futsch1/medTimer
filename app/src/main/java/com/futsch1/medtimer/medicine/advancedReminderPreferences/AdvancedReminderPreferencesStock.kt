package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.medicine.stockSettings.setupAmountEdit

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

    override fun onEntityUpdated(entity: Reminder) {
        super.onEntityUpdated(entity)

        menuProvider.medicineRepository = (preferenceManager.preferenceDataStore as ReminderDataStore).medicineRepository
        menuProvider.reminder = entity
    }

    override fun customSetup(entity: Reminder) {
        setupAmountEdit(findPreference("stock_threshold")!!)

        findPreference<Preference>("stock_threshold")?.isVisible = entity.reminderType == Reminder.ReminderType.OUT_OF_STOCK
        findPreference<Preference>("stock_reminder")?.isVisible = entity.reminderType == Reminder.ReminderType.OUT_OF_STOCK
        findPreference<Preference>("expiration_days_before")?.isVisible = entity.reminderType == Reminder.ReminderType.EXPIRATION_DATE
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