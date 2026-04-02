package com.futsch1.medtimer.helpers

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.text.bold
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineStringFormatter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesDataSource: PreferencesDataSource,
    private val timeFormatter: TimeFormatter
) {
    fun getMedicineNameWithStockText(fullMedicine: FullMedicineEntity): SpannableStringBuilder {
        return getMedicineNameWithStockText(preferencesDataSource.preferences.value, fullMedicine)
    }

    fun getMedicineNameWithStockText(userPreferences: UserPreferences, fullMedicine: FullMedicineEntity): SpannableStringBuilder {
        val builder = SpannableStringBuilder().bold {
            append(
                MedicineHelper.getMedicineName(
                    fullMedicine.medicine,
                    false,
                    userPreferences
                )
            )
        }
        builder.append(getStockTextWithIcons(fullMedicine))
        return builder
    }

    private fun getStockTextWithIcons(fullMedicine: FullMedicineEntity): SpannableStringBuilder {
        val builder = SpannableStringBuilder()

        val stockIconText = MedicineHelper.getStockIcons(fullMedicine)
        val stockText = if (fullMedicine.isStockManagementActive) getStockText(fullMedicine.medicine) else ""

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

    fun getDatesText(fullMedicine: FullMedicineEntity): SpannableStringBuilder {
        val s = SpannableStringBuilder()
        val medicine = fullMedicine.medicine

        if (medicine.productionDate != 0L) {
            s.append(context.getString(R.string.production_date))
            s.append(": ")
            s.append(timeFormatter.daysSinceEpochToDateString(medicine.productionDate))
        }
        if (medicine.expirationDate != 0L) {
            if (s.isNotEmpty()) {
                s.append(", ")
            }
            s.append(context.getString(R.string.expiration_date))
            s.append(": ")
            s.append(timeFormatter.daysSinceEpochToDateString(medicine.expirationDate))
            val expiredIcon = MedicineHelper.getExpiredIcon(fullMedicine)
            if (expiredIcon.isNotEmpty()) {
                s.append(" ")
                s.append(expiredIcon)
            }
        }
        return s
    }

    fun getStockText(medicine: MedicineEntity): String {
        return context.getString(
            R.string.medicine_stock_string,
            MedicineHelper.formatAmount(medicine.amount, medicine.unit)
        )
    }
}