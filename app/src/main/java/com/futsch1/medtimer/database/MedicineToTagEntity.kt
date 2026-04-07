package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "MedicineToTag", primaryKeys = ["medicineId", "tagId"])
class MedicineToTagEntity(var medicineId: Int, @field:ColumnInfo(index = true) var tagId: Int)

