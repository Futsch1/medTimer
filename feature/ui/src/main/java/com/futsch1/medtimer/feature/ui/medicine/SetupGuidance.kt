package com.futsch1.medtimer.feature.ui.medicine

import com.futsch1.medtimer.core.domain.model.Medicine

/** Setup help is derived only from saved medicine/reminder data, never from a selected Overview day. */
enum class SetupGuidance {
    ADD_MEDICINE,
    ADD_REMINDER,
    NONE
}

fun setupGuidance(medicines: List<Medicine>, selectedMedicineId: Int? = null): SetupGuidance {
    if (medicines.isEmpty()) return SetupGuidance.ADD_MEDICINE

    val selectedMedicine = selectedMedicineId?.let { id -> medicines.firstOrNull { it.id == id } }
    return if (selectedMedicine != null && selectedMedicine.reminders.isEmpty()) {
        SetupGuidance.ADD_REMINDER
    } else {
        SetupGuidance.NONE
    }
}
