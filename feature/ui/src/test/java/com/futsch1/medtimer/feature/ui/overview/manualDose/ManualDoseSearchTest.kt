package com.futsch1.medtimer.feature.ui.overview.manualDose

import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.PersistentData
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ManualDoseSearchTest {

    private fun viewModelWith(names: List<String>): ManualDoseViewModel {
        val medicineRepository: MedicineRepository = mock()
        whenever(medicineRepository.getAllFlow()).thenReturn(
            flowOf(names.mapIndexed { index, name -> Medicine.default().copy(id = index + 1, name = name) })
        )
        val persistentDataDataSource: PersistentDataDataSource = mock()
        whenever(persistentDataDataSource.data).thenReturn(MutableStateFlow(PersistentData.default()))
        return ManualDoseViewModel(medicineRepository, mock(), mock(), persistentDataDataSource, mock(), mock())
    }

    private val manyMedicines = listOf("Vitamin X 500 mg", "Medicine A", "Medicine B", "Supplement C", "vitamin D", "Medicine E")

    private suspend fun ManualDoseViewModel.labels() = state.first().medicineEntries.map { it.label }

    @Test
    fun `should list only medicines whose name contains the query, ignoring case`() = runTest {
        val viewModel = viewModelWith(manyMedicines)

        viewModel.search("VITAMIN")

        assertEquals(listOf("Vitamin X 500 mg", "vitamin D"), viewModel.labels())
    }

    @Test
    fun `should list all medicines again after reset`() = runTest {
        val viewModel = viewModelWith(manyMedicines)
        viewModel.search("vitamin")

        viewModel.reset()

        assertEquals(manyMedicines, viewModel.labels())
        assertEquals("", viewModel.searchQuery)
    }

    @Test
    fun `should offer the search field only when there are more medicines than fit the list`() = runTest {
        assertTrue(viewModelWith(manyMedicines.take(5)).state.first().showSearch)
        assertFalse(viewModelWith(manyMedicines.take(4)).state.first().showSearch)
    }
}
