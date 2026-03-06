package com.futsch1.medtimer.database

import android.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.futsch1.medtimer.ReminderNotificationChannelManager
import com.google.gson.annotations.Expose
import java.time.LocalDate
import java.util.Objects

@Entity
class Medicine @JvmOverloads constructor(name: String, id: Int = 0) {
    @JvmField
    @ColumnInfo(name = "medicineName")
    @Expose
    var name: String = ""

    @JvmField
    @PrimaryKey(autoGenerate = true)
    var medicineId: Int = 0

    @JvmField
    @ColumnInfo(defaultValue = "0xFFFF0000")
    @Expose
    var color: Int = Color.DKGRAY

    @JvmField
    @ColumnInfo(defaultValue = "false")
    @Expose
    var useColor: Boolean = false

    @JvmField
    @ColumnInfo(defaultValue = "3")
    @Expose
    var notificationImportance: Int = ReminderNotificationChannelManager.Importance.DEFAULT.value

    @JvmField
    @ColumnInfo(defaultValue = "0")
    @Expose
    var iconId: Int = 0

    @JvmField
    @ColumnInfo(defaultValue = "0")
    @Expose
    var amount: Double = 0.0

    @JvmField
    @ColumnInfo(defaultValue = "[]")
    @Expose
    var refillSizes: MutableList<Double> = mutableListOf()

    @JvmField
    @ColumnInfo(defaultValue = "")
    @Expose
    var unit: String = ""

    @JvmField
    @ColumnInfo(defaultValue = "1.0")
    @Expose
    var sortOrder: Double = 1.0

    @JvmField
    @ColumnInfo(defaultValue = "")
    @Expose
    var notes: String? = ""

    @JvmField
    @ColumnInfo(defaultValue = "false")
    @Expose
    var showNotificationAsAlarm: Boolean = false

    @JvmField
    @ColumnInfo(defaultValue = "0")
    @Expose
    var productionDate: Long = 0

    @JvmField
    @ColumnInfo(defaultValue = "0")
    @Expose
    var expirationDate: Long = 0

    @Ignore
    constructor() : this("")

    init {
        this.name = name
        this.medicineId = id
    }

    fun hasExpired(): Boolean {
        return expirationDate != 0L && expirationDate < LocalDate.now().toEpochDay()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false
        return membersEqual(other as Medicine)
    }

    override fun hashCode(): Int {
        return Objects.hash(medicineId, name, useColor, color, notificationImportance, iconId, amount, refillSizes, unit, notes, showNotificationAsAlarm)
    }

    private fun membersEqual(that: Medicine): Boolean {
        return medicineId == that.medicineId &&
                name == that.name && useColor == that.useColor && color == that.color && notificationImportance == that.notificationImportance && iconId == that.iconId && amount == that.amount &&
                refillSizes == that.refillSizes &&
                unit == that.unit &&
                notes == that.notes && showNotificationAsAlarm == that.showNotificationAsAlarm && expirationDate == that.expirationDate && productionDate == that.productionDate
    }

    val refillSize: Double
        get() = (if (refillSizes.isEmpty()) 0.0 else refillSizes[0])
}
