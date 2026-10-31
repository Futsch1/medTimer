package com.futsch1.medtimer.core.common.helpers

import android.text.SpannableStringBuilder
import androidx.core.text.bold
import androidx.core.text.color
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.UserPreferences
import java.text.NumberFormat
import java.util.regex.Pattern

object MedicineHelper {
    private val CYCLIC_COUNT: Pattern = Pattern.compile(" (\\(\\d+/\\d+)\\)")

    // NumberFormat is not thread-safe; ThreadLocal gives each thread its own instance at zero per-call cost
    private val numberFormat: ThreadLocal<NumberFormat> = ThreadLocal.withInitial { NumberFormat.getNumberInstance() }

    fun normalizeMedicineName(medicineName: String): String {
        return CYCLIC_COUNT.matcher(medicineName).replaceAll("")
    }

    fun getMedicineName(
        medicine: Medicine,
        notification: Boolean,
        userPreferences: UserPreferences
    ): String {
        return if (userPreferences.hideMedicineName && notification) {
            medicine.name[0] + "*".repeat(medicine.name.length - 1)
        } else {
            medicine.name
        }
    }

    fun getStockIcons(medicine: Medicine): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        if (medicine.isOutOfStock()) {
            builder.color(0xffcc0000.toInt()) { bold { append("⚠") } }
        }
        val expiredIcon = getExpiredIcon(medicine)
        if (expiredIcon.isNotEmpty()) {
            if (builder.isNotEmpty()) {
                builder.append(" ")
            }
            builder.append(expiredIcon)
        }
        return builder
    }

    fun getExpiredIcon(medicine: Medicine): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        if (medicine.hasExpired()) {
            builder.color(0xffcc0000.toInt()) { bold { append("\uD83D\uDEAB") } }
        }
        return builder
    }

    fun getUnit(medicine: Medicine): String {
        val doseAmounts = medicine.reminders
            .filter { it.active && !it.isOutOfStockOrExpirationReminder }
            .map { it.amount }
        return DoseAmountParser.summarize(doseAmounts).unit ?: ""
    }

    fun formatAmount(amount: Double, medicine: Medicine): String = formatAmount(amount, getUnit(medicine))

    fun formatAmount(amount: Double, unit: String): String {
        val fmt = numberFormat.get() ?: return ""
        fmt.minimumFractionDigits = 0
        fmt.maximumFractionDigits = 2
        return fmt.format(amount) + if (unit.isEmpty()) "" else " $unit"
    }
}
