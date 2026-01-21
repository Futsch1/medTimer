package com.futsch1.medtimer.helpers

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import com.futsch1.medtimer.database.MedicineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class EntityDataStore<T> : PreferenceDataStore() {
    abstract var entity: T
    abstract val entityId: Int
}

abstract class EntityViewModel<T>(application: Application) : AndroidViewModel(application) {
    abstract fun getFlow(id: Int): Flow<T?>
    abstract val medicineRepository: MedicineRepository
}

abstract class EntityPreferencesFragment<T>(
    val preferencesResId: Int,
    val links: Map<String, (Int) -> NavDirections>,
    open val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>,
    val simpleSummaryKeys: List<String>,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : PreferenceFragmentCompat() {
    lateinit var dataStore: EntityDataStore<T>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        postponeEnterTransition()

        this.lifecycleScope.launch(ioDispatcher) {
            try {
                dataStore = getEntityDataStore(requireArguments())

                preferenceManager.preferenceDataStore = dataStore
                this.launch(mainDispatcher) {
                    setPreferencesFromResource(preferencesResId, rootKey)
                    observeUserData()
                    setupLinks()
                    setupOnClick()
                    customSetup(dataStore.entity)

                    startPostponedEnterTransition()
                }
            } catch (_: NullPointerException) {
                // It may happen that the reminder is deleted already, so ignore this
            }
        }
    }

    abstract fun getEntityDataStore(
        requireArguments: Bundle
    ): EntityDataStore<T>

    abstract fun getEntityViewModel(): EntityViewModel<T>

    abstract fun customSetup(entity: T)

    private fun observeUserData() {
        lifecycleScope.launch {
            getEntityViewModel().getFlow(dataStore.entityId).collect { entity ->
                if (entity == null) {
                    return@collect
                }
                dataStore.entity = entity
                onEntityUpdated(entity)
            }
        }
    }

    private fun setupLinks() {
        try {
            val navController = findNavController()

            for (link in links) {
                val preference = findPreference<Preference?>(link.key)
                preference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
                    try {
                        navController.navigate(link.value(dataStore.entityId))
                    } catch (_: IllegalArgumentException) {
                        // Intentionally empty (monkey test can cause this to fail)
                    }
                    true
                }
            }
        } catch (_: IllegalStateException) {
            // Ignore this
        }
    }

    private fun setupOnClick() {
        for (onClick in customOnClick) {
            val preference = findPreference<Preference?>(onClick.key)
            preference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
                onClick.value(requireActivity(), preference)
                true
            }
        }
    }

    open fun onEntityUpdated(entity: T) {
        for (simpleSummaryKey in simpleSummaryKeys) {
            val preference = findPreference<Preference?>(simpleSummaryKey)
            preference!!.summary = preferenceManager.preferenceDataStore?.getString(simpleSummaryKey, "?")
        }
    }
}