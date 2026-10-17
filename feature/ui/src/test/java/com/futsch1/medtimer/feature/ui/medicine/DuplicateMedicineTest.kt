package com.futsch1.medtimer.feature.ui.medicine

import com.futsch1.medtimer.core.domain.model.Medicine
import org.junit.Assert.assertEquals
import org.junit.Test

class DuplicateMedicineTest {

    @Test
    fun `should mark the duplicate's name as a copy and keep everything else`() {
        val original = Medicine.default().copy(id = 4, name = "Vitamin X 500 mg", amount = 30.0, unit = "pills")

        val duplicate = duplicateOf(original) { "$it (copy)" }

        assertEquals(original.copy(id = 0, name = "Vitamin X 500 mg (copy)"), duplicate)
    }
}
