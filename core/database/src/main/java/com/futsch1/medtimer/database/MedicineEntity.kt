package com.futsch1.medtimer.database

import android.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.futsch1.medtimer.core.domain.model.Medicine

@Entity(tableName = "Medicine")
class MedicineEntity(
    @ColumnInfo(name = "medicineName") var name: String? = null,
    @PrimaryKey(autoGenerate = true) var medicineId: Int = 0,
    @ColumnInfo(defaultValue = "0xFFFF0000") var color: Int = Color.DKGRAY,
    @ColumnInfo(defaultValue = "false") var useColor: Boolean = false,
    @ColumnInfo(defaultValue = "3") var notificationImportance: Int = Medicine.NotificationImportance.DEFAULT.value,
    @ColumnInfo(defaultValue = "0") var iconId: Int = 0,
    @ColumnInfo(defaultValue = "0") var amount: Double = 0.0,
    @ColumnInfo(defaultValue = "[]") var refillSizes: MutableList<Double>? = null,
    @ColumnInfo(defaultValue = "") var unit: String? = null,
    @ColumnInfo(defaultValue = "1.0") var sortOrder: Double = 1.0,
    @ColumnInfo(defaultValue = "") var notes: String? = "",
    @ColumnInfo(defaultValue = "false") var showNotificationAsAlarm: Boolean = false,
    @ColumnInfo(defaultValue = "0") var productionDate: Long = 0,
    @ColumnInfo(defaultValue = "0") var expirationDate: Long = 0,
    @ColumnInfo(defaultValue = "false") var cannotBeSkipped: Boolean = false,
    @ColumnInfo(defaultValue = "") var prescriptionContact: String? = ""
)
