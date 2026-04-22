package com.futsch1.medtimer.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.google.gson.annotations.Expose

class FullMedicineEntity(
    @Embedded
    @Expose
    var medicine: MedicineEntity = MedicineEntity(),
    @Relation(parentColumn = "medicineId", entityColumn = "tagId", associateBy = Junction(MedicineToTagEntity::class))
    @Expose
    var tags: List<TagEntity> = listOf(),
    @Relation(parentColumn = "medicineId", entityColumn = "medicineRelId")
    @Expose
    var reminders: MutableList<ReminderEntity> = mutableListOf()
)
