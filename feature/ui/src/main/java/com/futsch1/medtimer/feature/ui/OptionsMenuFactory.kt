package com.futsch1.medtimer.feature.ui

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import com.futsch1.medtimer.core.common.helpers.EntityEditOptionsMenu

/**
 * Abstraction over the app-shell `OptionsMenu` so the UI fragments don't depend on `:app`.
 * Bound to `OptionsMenu.Factory` via Hilt in `:app`.
 */
interface OptionsMenuFactory {
    fun create(
        fragment: Fragment,
        navController: NavController,
        hideFilter: Boolean,
        medicineViewModel: MedicineViewModel
    ): EntityEditOptionsMenu
}
