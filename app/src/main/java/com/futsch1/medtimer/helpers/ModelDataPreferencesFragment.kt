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
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

abstract class ModelDataStore<T> : PreferenceDataStore() {
    abstract var modelData: T
    abstract val modelDataId: Int
}

abstract class ModelDataViewModel<T> : ViewModel() {
    abstract fun getFlow(id: Int): Flow<T?>
}

abstract class ModelDataPreferencesFragment<T>(
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

    lateinit var dataStore: ModelDataStore<T>

    var idlingResource: SimpleIdlingResource = SimpleIdlingResource("ModelDataPreferencesFragment_${preferencesResId}")

    init {
        idlingResource.setBusy()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        postponeEnterTransition()

        this.lifecycleScope.launch(ioDispatcher) {
            try {
                dataStore = getDataStore(requireArguments())
                preferenceManager.preferenceDataStore = dataStore
            } catch (_: NullPointerException) {
                // It may happen that the reminder is deleted already, so ignore this
            }

            withContext(mainDispatcher) {
                setPreferencesFromResource(preferencesResId, rootKey)
                observeUserData()
                setupLinks()
                setupOnClick()
                customSetup(dataStore.modelData)

                startPostponedEnterTransition()
                idlingResource.setIdle()
            }
        }
    }

    abstract suspend fun getDataStore(
        requireArguments: Bundle
    ): ModelDataStore<T>

    abstract fun getEntityViewModel(): ModelDataViewModel<T>

    abstract fun customSetup(modelData: T)

    private fun observeUserData() {
        lifecycleScope.launch {
            getEntityViewModel().getFlow(dataStore.modelDataId).collect { modelData ->
                if (modelData == null) {
                    return@collect
                }
                dataStore.modelData = modelData
                onModelDataUpdated(modelData)
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
                        navController.navigate(link.value(dataStore.modelDataId))
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

    open fun onModelDataUpdated(modelData: T) {
        for (simpleSummaryKey in simpleSummaryKeys) {
            val preference = findPreference<Preference?>(simpleSummaryKey)
            preference!!.summary = preferenceManager.preferenceDataStore?.getString(simpleSummaryKey, "?")
        }
    }
}