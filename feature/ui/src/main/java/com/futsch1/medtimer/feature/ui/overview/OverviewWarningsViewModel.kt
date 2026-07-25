package com.futsch1.medtimer.feature.ui.overview

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.common.di.IsDebugBuild
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Drives the Overview screen's warning cards. Following `docs/guidelines/jetpack-compose.md` §State holders, the
 * view-model owns a [MutableOverviewWarningsState] and exposes only the read-only [state].
 *
 * [PowerManager.isIgnoringBatteryOptimizations] is a synchronous query with no change notification —
 * the user grants the exemption in system settings and returns — so [refresh] must also be called
 * from `onResume`, not just on flow emissions.
 */
@HiltViewModel
class OverviewWarningsViewModel @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val persistentDataDataSource: PersistentDataDataSource,
    private val powerManager: PowerManager,
    @param:ApplicationContext private val context: Context,
    @param:IsDebugBuild private val isDebugBuild: Boolean,
) : ViewModel() {

    private val _state = MutableOverviewWarningsState()
    val state: OverviewWarningsState get() = _state

    init {
        combine(preferencesDataSource.preferences, persistentDataDataSource.data) { _, _ -> }
            .onEach { refresh() }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        val persistentData = persistentDataDataSource.data.value
        _state.showBatteryOptimizationWarning = showBatteryOptimizationWarning(
            warningsSuppressed = isDebugBuild,
            isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName),
            batteryWarningShown = persistentData.batteryWarningShown,
        )
        _state.showExactRemindersWarning = showExactRemindersWarning(
            warningsSuppressed = isDebugBuild,
            exactReminders = preferencesDataSource.preferences.value.exactReminders,
            exactRemindersWarningShown = persistentData.exactRemindersWarningShown,
        )
    }

    fun dismissBatteryWarning() {
        persistentDataDataSource.setBatteryWarningShown(true)
        refresh()
    }

    fun dismissExactRemindersWarning() {
        persistentDataDataSource.setExactRemindersWarningShown(true)
        refresh()
    }
}
