package com.futsch1.medtimer.database.toModel

import com.futsch1.medtimer.core.domain.model.MedicineLabel
import com.futsch1.medtimer.database.MedicineLabelEntity

fun MedicineLabelEntity.toModel(): MedicineLabel = MedicineLabel(text = text, medicineId = medicineId)

fun MedicineLabel.toEntity(): MedicineLabelEntity = MedicineLabelEntity(text = text, medicineId = medicineId)
