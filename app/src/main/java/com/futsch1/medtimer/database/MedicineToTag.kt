package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["medicineId", "tagId"])
class MedicineToTag(@JvmField var medicineId: Int, @JvmField @field:ColumnInfo(index = true) var tagId: Int)

