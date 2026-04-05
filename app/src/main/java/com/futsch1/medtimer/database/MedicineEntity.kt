package com.futsch1.medtimer.database

import android.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.futsch1.medtimer.ReminderNotificationChannelManager
import com.google.gson.annotations.Expose

@Entity(tableName = "Medicine")
class MedicineEntity() {
    @ColumnInfo(name = "medicineName")
    @Expose
    var name: String = ""

    @PrimaryKey(autoGenerate = true)
    var medicineId: Int = 0

    @ColumnInfo(defaultValue = "0xFFFF0000")
    @Expose
    var color: Int = Color.DKGRAY

    @ColumnInfo(defaultValue = "false")
    @Expose
    var useColor: Boolean = false

    @ColumnInfo(defaultValue = "3")
    @Expose
    var notificationImportance: Int = ReminderNotificationChannelManager.Importance.DEFAULT.value

    @ColumnInfo(defaultValue = "0")
    @Expose
    var iconId: Int = 0

    @ColumnInfo(defaultValue = "0")
    @Expose
    var amount: Double = 0.0

    @ColumnInfo(defaultValue = "[]")
    @Expose
    var refillSizes: MutableList<Double> = mutableListOf()

    @ColumnInfo(defaultValue = "")
    @Expose
    var unit: String = ""

    @ColumnInfo(defaultValue = "1.0")
    @Expose
    var sortOrder: Double = 1.0

    @ColumnInfo(defaultValue = "")
    @Expose
    var notes: String? = ""

    @ColumnInfo(defaultValue = "false")
    @Expose
    var showNotificationAsAlarm: Boolean = false

    @ColumnInfo(defaultValue = "0")
    @Expose
    var productionDate: Long = 0

    @ColumnInfo(defaultValue = "0")
    @Expose
    var expirationDate: Long = 0

    @Ignore
    constructor(name: String, id: Int = 0) : this() {
        this.name = name
        this.medicineId = id
    }

    @Ignore
    constructor(name: String) : this() {
        this.name = name
    }

}
