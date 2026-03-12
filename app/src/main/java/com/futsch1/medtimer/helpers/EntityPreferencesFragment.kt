package com.futsch1.medtimer.helpers

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

abstract class EntityDataStore<T> : PreferenceDataStore() {
    abstract var entity: T
    abstract val entityId: Int
}

abstract class EntityViewModel<T> : ViewModel() {
    abstract fun getFlow(id: Int): Flow<T?>

    // TODO: view model should not expose the repository to the view logic; the repository be private
    abstract val medicineRepository: MedicineRepository
}

abstract class EntityPreferencesFragment<T>(
    val preferencesResId: Int,
    val links: Map<String, (Int) -> NavDirections>,
    open val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>,
    val simpleSummaryKeys: List<String>
) : PreferenceFragmentCompat() {
    @Inject
    @Dispatcher(MedTimerDispatchers.IO)
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @Dispatcher(MedTimerDispatchers.Main)
    lateinit var mainDispatcher: CoroutineDispatcher

    lateinit var dataStore: EntityDataStore<T>
    abstract val medicineRepository: MedicineRepository

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        postponeEnterTransition()

        this.lifecycleScope.launch(ioDispatcher) {
            try {
                dataStore = getEntityDataStore(requireArguments())
                preferenceManager.preferenceDataStore = dataStore
            } catch (_: NullPointerException) {
                // It may happen that the reminder is deleted already, so ignore this
            }

            withContext(mainDispatcher) {
                setPreferencesFromResource(preferencesResId, rootKey)
                observeUserData()
                setupLinks()
                setupOnClick()
                customSetup(dataStore.entity)

                startPostponedEnterTransition()
            }
        }
    }

    abstract suspend fun getEntityDataStore(
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