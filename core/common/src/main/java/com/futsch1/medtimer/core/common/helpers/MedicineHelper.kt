package com.futsch1.medtimer.core.common.helpers

import android.text.SpannableStringBuilder
import androidx.core.text.bold
import androidx.core.text.color
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.UserPreferences
import java.text.NumberFormat
import java.text.ParseException
import java.text.ParsePosition
import java.util.regex.Pattern

object MedicineHelper {
    private val CYCLIC_COUNT: Pattern = Pattern.compile(" (\\(\\d+/\\d+)\\)")
    private val AMOUNT_PATTERN: Pattern = Pattern.compile("(?:\\d|\\.\\d)[.,\\s\\d]*")
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

    fun formatAmount(amount: Double, unit: String): String {
        val fmt = numberFormat.get() ?: return ""
        fmt.minimumFractionDigits = 0
        fmt.maximumFractionDigits = 2
        return fmt.format(amount) + if (unit.isEmpty()) "" else " $unit"
    }

    /**
     * Parses the first number in [amount] with the device's number format.
     *
     * By default, the number format reads as much of the number as it can, so "10.5" is read as 10 if "." is not a separator in
     * the device's locale. With [requireComplete], null is returned instead unless the whole number is read (separators at its
     * end, as in "5. mg", are ignored).
     */
    fun parseAmount(amount: String?, requireComplete: Boolean = false): Double? =
        numberFormat.get()?.let { parseAmount(amount, it, requireComplete) }

    /** [parseAmount] with an explicit number [format], e.g. to test other locales. */
    fun parseAmount(amount: String?, format: NumberFormat, requireComplete: Boolean): Double? {
        val matcher = AMOUNT_PATTERN.matcher(amount ?: "")
        if (!matcher.find()) {
            return null
        }
        val number = matcher.group(0)!!.replace(" ", "")
        if (!requireComplete) {
            return try {
                format.parse(number)?.toDouble()
            } catch (_: ParseException) {
                null
            }
        }
        val complete = number.trimEnd('.', ',')
        val position = ParsePosition(0)
        val parsed = format.parse(complete, position) ?: return null
        return parsed.toDouble().takeIf { position.index == complete.length }
    }
}
