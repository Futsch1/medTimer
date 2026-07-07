package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A normalized snippet of OCR'd package text mapped to a medicine. The text itself is the
 * primary key, so remembering the same snippet twice cannot create duplicates. Deleting a
 * medicine cascades to its remembered labels.
 */
@Entity(
    tableName = "MedicineLabel",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["medicineId"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicineId"])]
)
class MedicineLabelEntity(
    @PrimaryKey var text: String,
    @ColumnInfo(name = "medicineId") var medicineId: Int
)
