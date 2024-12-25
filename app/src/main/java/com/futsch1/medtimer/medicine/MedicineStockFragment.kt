package com.futsch1.medtimer.medicine

import android.annotation.SuppressLint
import android.view.View
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.helpers.DatabaseEntityEditFragment
import com.futsch1.medtimer.helpers.MedicineEntityInterface
import com.google.android.material.textfield.TextInputEditText

class MedicineStockFragment :
    DatabaseEntityEditFragment<Medicine>(
        MedicineEntityInterface(),
        R.layout.fragment_medicine_stock
    ) {

    override fun getEntityId(): Int {
        return MedicineStockFragmentArgs.fromBundle(requireArguments()).medicineId
    }

    override fun fillEntityData(entity: Medicine, fragmentView: View) {
        try {
            entity.medicationAmount =
                fragmentView.findViewById<TextInputEditText>(R.id.amountLeft).text.toString()
                    .toInt()
        } catch (e: NumberFormatException) {
            // Empty for now
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onEntityLoaded(entity: Medicine, fragmentView: View) {
        fragmentView.findViewById<TextInputEditText>(R.id.amountLeft)
            .setText(entity.medicationAmount.toString())
    }
}