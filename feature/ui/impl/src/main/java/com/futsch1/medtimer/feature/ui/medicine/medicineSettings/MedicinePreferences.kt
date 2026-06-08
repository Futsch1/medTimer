package com.futsch1.medtimer.feature.ui.medicine.medicineSettings

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.preference.Preference
import com.futsch1.medtimer.core.common.helpers.ModelDataPreferencesFragment
import com.futsch1.medtimer.core.common.helpers.ModelDataStore
import com.futsch1.medtimer.core.common.helpers.ModelDataViewModel
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import javax.inject.Inject

abstract class MedicinePreferences(
    preferencesResId: Int,
    links: Map<String, (Int) -> NavDirections>,
    customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>,
    simpleSummaryKeys: List<String>
) : ModelDataPreferencesFragment<Medicine>(
    preferencesResId,
    links,
    customOnClick,
    simpleSummaryKeys
) {
    @Inject
    lateinit var medicineRepository: MedicineRepository

    @Inject
    lateinit var medicineDataStoreFactory: MedicineDataStore.Factory

    private val medicineViewModel: MedicineViewModel by viewModels()

    override suspend fun getDataStore(requireArguments: Bundle): ModelDataStore<Medicine> {
        val entityId = requireArguments.getInt("medicineId")
        val entity = medicineRepository.fetch(entityId)!!
        return medicineDataStoreFactory.create(entity)
    }

    override fun getEntityViewModel(): ModelDataViewModel<Medicine> = medicineViewModel

}