package com.futsch1.medtimer.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

class FullMedicineEntity(
    @Embedded
    var medicine: MedicineEntity = MedicineEntity(),
    @Relation(parentColumn = "medicineId", entityColumn = "tagId", associateBy = Junction(MedicineToTagEntity::class))
    var tags: List<TagEntity> = listOf(),
    @Relation(parentColumn = "medicineId", entityColumn = "medicineRelId")
    var reminders: MutableList<ReminderEntity> = mutableListOf()
)
