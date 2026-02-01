package com.futsch1.medtimer.medicine

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.MedicineHelper.getMedicineNameWithStockText
import com.futsch1.medtimer.helpers.ViewColorHelper
import com.futsch1.medtimer.helpers.getActiveReminders
import com.futsch1.medtimer.helpers.remindersSummary
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicineViewHolder private constructor(
    holderItemView: View,
    private val activity: FragmentActivity,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
) :
    RecyclerView.ViewHolder(holderItemView) {
    private val medicineNameView: TextView = holderItemView.findViewById(R.id.medicineName)
    private val remindersSummaryView: TextView = holderItemView.findViewById(R.id.remindersSummary)
    private val tags: FlexboxLayout = holderItemView.findViewById(R.id.tags)

    fun bind(medicine: FullMedicine) {
        medicineNameView.text = getMedicineNameWithStockText(itemView.context, medicine)
        setupSummary(medicine)

        itemView.setOnClickListener { _: View? -> navigateToEditFragment(medicine) }

        if (medicine.medicine.useColor) {
            ViewColorHelper.setViewBackground(itemView, listOf(medicineNameView, remindersSummaryView), medicine.medicine.color)
        } else {
            ViewColorHelper.setDefaultColors(itemView, listOf(medicineNameView, remindersSummaryView))
        }

        ViewColorHelper.setIconToImageView(itemView, itemView.findViewById(R.id.medicineIcon), medicine.medicine.iconId)

        buildTags(medicine.tags)
    }

    private fun setupSummary(medicine: FullMedicine) {
        val activeReminders: List<Reminder> = getActiveReminders(medicine)
        if (activeReminders.isEmpty()) {
            if (medicine.reminders.isEmpty()) {
                remindersSummaryView.setText(R.string.no_reminders)
            } else {
                remindersSummaryView.setText(R.string.inactive)
            }
        } else {
            activity.lifecycleScope.launch(dispatcher) {
                val summary = remindersSummary(activeReminders, itemView.context)
                activity.runOnUiThread { remindersSummaryView.text = summary }
            }
        }
    }

    private fun navigateToEditFragment(medicine: FullMedicine) {
        val navController = findNavController(itemView)
        val action = MedicinesFragmentDirections.actionMedicinesFragmentToEditMedicineFragment(
            medicine.medicine.medicineId
        )
        try {
            navController.navigate(action)
        } catch (_: IllegalArgumentException) {
            // Ignore
        }
    }

    fun buildTags(tagsList: List<Tag>) {
        tags.removeAllViews()
        for (tag in tagsList) {
            @SuppressLint("InflateParams") val chip = LayoutInflater.from(itemView.context).inflate(R.layout.tag, null) as Chip
            chip.text = tag.name
            chip.isChecked = true
            chip.isCheckable = false
            chip.isCloseIconVisible = false
            chip.setOnClickListener { _: View? -> itemView.performClick() }
            chip.rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
            val params = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val margin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4f, this.itemView.resources.displayMetrics
            ).toInt()
            params.setMargins(margin, 0, margin, 0)
            chip.setLayoutParams(params)
            tags.addView(chip)
        }
    }

    companion object {
        @JvmStatic
        fun create(parent: ViewGroup, activity: FragmentActivity): MedicineViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_medicine, parent, false)
            return MedicineViewHolder(view, activity)
        }
    }
}
