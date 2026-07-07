package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A scanned barcode mapped to a medicine. The barcode string itself is the primary key,
 * so scanning the same code twice cannot create duplicates. Deleting a medicine cascades
 * to its barcodes.
 */
@Entity(
    tableName = "Barcode",
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
class BarcodeEntity(
    @PrimaryKey var barcode: String,
    @ColumnInfo(name = "medicineId") var medicineId: Int
)
