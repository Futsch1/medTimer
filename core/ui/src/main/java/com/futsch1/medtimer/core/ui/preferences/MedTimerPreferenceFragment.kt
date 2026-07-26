package com.futsch1.medtimer.core.ui.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import com.futsch1.medtimer.core.ui.component.withTopAppBar

/** Base for the plain preference screens, giving each one its own top bar. */
abstract class MedTimerPreferenceFragment : PreferenceFragmentCompat() {
    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = withTopAppBar(super.onCreateView(inflater, container, savedInstanceState))
}
