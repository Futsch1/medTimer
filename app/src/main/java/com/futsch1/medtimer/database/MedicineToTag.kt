package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["medicineId", "tagId"])
class MedicineToTag(var medicineId: Int, @field:ColumnInfo(index = true) var tagId: Int)

