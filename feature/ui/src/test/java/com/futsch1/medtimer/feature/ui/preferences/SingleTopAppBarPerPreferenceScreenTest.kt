package com.futsch1.medtimer.feature.ui.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.futsch1.medtimer.core.ui.preferences.MedTimerPreferenceFragment
import com.futsch1.medtimer.core.ui.preferences.ModelDataPreferencesFragment
import com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences.AdvancedReminderPreferencesCyclicFragment
import com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences.AdvancedReminderPreferencesInstructionsFragment
import com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences.AdvancedReminderPreferencesRootFragment
import com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences.AdvancedReminderPreferencesStatusFragment
import com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences.AdvancedReminderPreferencesStockFragment
import com.futsch1.medtimer.feature.ui.medicine.medicineSettings.MedicineSettingsFragment
import com.futsch1.medtimer.feature.ui.medicine.medicineSettings.StockSettingsFragment
import org.junit.Test
import kotlin.test.assertEquals

/**
 * The preference bases already stack a top bar onto the fragment's view. A subclass that overrides
 * onCreateView and wraps the super result again renders a second bar on top of the first.
 */
class SingleTopAppBarPerPreferenceScreenTest {
    private val preferenceScreens: List<Class<out Fragment>> = listOf(
        AdvancedReminderPreferencesRootFragment::class.java,
        AdvancedReminderPreferencesStockFragment::class.java,
        AdvancedReminderPreferencesCyclicFragment::class.java,
        AdvancedReminderPreferencesStatusFragment::class.java,
        AdvancedReminderPreferencesInstructionsFragment::class.java,
        MedicineSettingsFragment::class.java,
        StockSettingsFragment::class.java,
        PrivacyPreferencesFragment::class.java,
        RepeatRemindersPreferencesFragment::class.java,
        WeekendModePreferencesFragment::class.java,
        PreferencesFragment::class.java,
    )

    @Test
    fun preferenceScreensWrapTheirViewInATopAppBarExactlyOnce() {
        val basesThatWrap = setOf(
            ModelDataPreferencesFragment::class.java,
            MedTimerPreferenceFragment::class.java,
        )

        for (screen in preferenceScreens) {
            val declaringClass = screen.getMethod(
                "onCreateView",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Bundle::class.java
            ).declaringClass

            assertEquals(
                true,
                declaringClass in basesThatWrap,
                "${screen.simpleName} overrides onCreateView in $declaringClass, " +
                        "stacking a second top app bar onto the one its base already adds"
            )
        }
    }
}
