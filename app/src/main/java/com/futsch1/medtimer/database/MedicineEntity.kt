package com.futsch1.medtimer.database

import android.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.futsch1.medtimer.model.Medicine
import com.google.gson.annotations.Expose

@Entity(tableName = "Medicine")
class MedicineEntity(
    @ColumnInfo(name = "medicineName") @field:Expose var name: String? = null,
    @PrimaryKey(autoGenerate = true) var medicineId: Int = 0,
    @ColumnInfo(defaultValue = "0xFFFF0000") @field:Expose var color: Int = Color.DKGRAY,
    @ColumnInfo(defaultValue = "false") @field:Expose var useColor: Boolean = false,
    @ColumnInfo(defaultValue = "3") @field:Expose var notificationImportance: Int = Medicine.NotificationImportance.DEFAULT.value,
    @ColumnInfo(defaultValue = "0") @field:Expose var iconId: Int = 0,
    @ColumnInfo(defaultValue = "0") @field:Expose var amount: Double = 0.0,
    @ColumnInfo(defaultValue = "[]") @field:Expose var refillSizes: MutableList<Double>? = null,
    @ColumnInfo(defaultValue = "") @field:Expose var unit: String? = null,
    @ColumnInfo(defaultValue = "1.0") @field:Expose var sortOrder: Double = 1.0,
    @ColumnInfo(defaultValue = "") @field:Expose var notes: String? = "",
    @ColumnInfo(defaultValue = "false") @field:Expose var showNotificationAsAlarm: Boolean = false,
    @ColumnInfo(defaultValue = "0") @field:Expose var productionDate: Long = 0,
    @ColumnInfo(defaultValue = "0") @field:Expose var expirationDate: Long = 0,
)
