package com.futsch1.medtimer.helpers

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.text.bold
import com.futsch1.medtimer.R
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineStringFormatter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesDataSource: PreferencesDataSource,
    private val timeFormatter: TimeFormatter
) {
    fun getMedicineNameWithStockText(medicine: Medicine): SpannableStringBuilder {
        return getMedicineNameWithStockText(preferencesDataSource.preferences.value, medicine)
    }

    fun getMedicineNameWithStockText(userPreferences: UserPreferences, medicine: Medicine): SpannableStringBuilder {
        val builder = SpannableStringBuilder().bold {
            append(
                MedicineHelper.getMedicineName(
                    medicine,
                    false,
                    userPreferences
                )
            )
        }
        builder.append(getStockTextWithIcons(medicine))
        return builder
    }

    private fun getStockTextWithIcons(medicine: Medicine): SpannableStringBuilder {
        val builder = SpannableStringBuilder()

        val stockIconText = MedicineHelper.getStockIcons(medicine)
        val stockText = if (medicine.isStockManagementActive()) getStockText(medicine) else ""

        if (stockIconText.isNotEmpty() || stockText.isNotEmpty()) {
            builder.append(" (")
            builder.append(stockText)
            if (stockText.isNotEmpty() && stockIconText.isNotEmpty())
                builder.append(" ")
            builder.append(stockIconText)
            builder.append(")")
        }
        return builder
    }

    fun getDatesText(medicine: Medicine): SpannableStringBuilder {
        val s = SpannableStringBuilder()

        if (medicine.productionDate != LocalDate.EPOCH) {
            s.append(context.getString(R.string.production_date))
            s.append(": ")
            s.append(timeFormatter.localDateToString(medicine.productionDate))
        }
        if (medicine.expirationDate != LocalDate.EPOCH) {
            if (s.isNotEmpty()) {
                s.append(", ")
            }
            s.append(context.getString(R.string.expiration_date))
            s.append(": ")
            s.append(timeFormatter.localDateToString(medicine.expirationDate))
            val expiredIcon = MedicineHelper.getExpiredIcon(medicine)
            if (expiredIcon.isNotEmpty()) {
                s.append(" ")
                s.append(expiredIcon)
            }
        }
        return s
    }

    fun getStockText(medicine: Medicine): String {
        return context.getString(
            R.string.medicine_stock_string,
            MedicineHelper.formatAmount(medicine.amount, medicine.unit)
        )
    }
}