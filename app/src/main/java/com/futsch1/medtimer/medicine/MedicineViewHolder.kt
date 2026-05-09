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
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.helpers.MedicineStringFormatter
import com.futsch1.medtimer.helpers.ReminderSummaryFormatter
import com.futsch1.medtimer.helpers.ViewColorHelper
import com.futsch1.medtimer.helpers.getActiveReminders
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.Tag
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicineViewHolder @AssistedInject constructor(
    @Assisted parent: ViewGroup,
    @Assisted private val activity: FragmentActivity,
    private val medicineStringFormatter: MedicineStringFormatter,
    private val reminderSummaryFormatter: ReminderSummaryFormatter,
    private val medicineIcons: MedicineIcons,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher,
    @param:Dispatcher(MedTimerDispatchers.Main) private val mainDispatcher: CoroutineDispatcher
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.card_medicine, parent, false)) {

    @AssistedFactory
    interface Factory {
        fun create(parent: ViewGroup, activity: FragmentActivity): MedicineViewHolder
    }

    private val medicineNameView: TextView = itemView.findViewById(R.id.medicineName)
    private val remindersSummaryView: TextView = itemView.findViewById(R.id.remindersSummary)
    private val tags: FlexboxLayout = itemView.findViewById(R.id.tags)

    fun bind(medicine: Medicine) {
        medicineNameView.text = medicineStringFormatter.getMedicineNameWithStockText(medicine)
        setupSummary(medicine)

        itemView.setOnClickListener { _: View? -> navigateToEditFragment(medicine) }

        if (medicine.useColor) {
            ViewColorHelper.setViewBackground(itemView, listOf(medicineNameView, remindersSummaryView), medicine.color)
        } else {
            ViewColorHelper.setDefaultColors(itemView, listOf(medicineNameView, remindersSummaryView))
        }

        ViewColorHelper.setIconToImageView(medicineIcons, itemView, itemView.findViewById(R.id.medicineIcon), medicine.iconId)

        buildTags(medicine.tags)
    }

    private fun setupSummary(medicine: Medicine) {
        val activeReminders: List<Reminder> = getActiveReminders(medicine)
        if (activeReminders.isEmpty()) {
            if (medicine.reminders.isEmpty()) {
                remindersSummaryView.setText(R.string.no_reminders)
            } else {
                remindersSummaryView.setText(R.string.inactive)
            }
        } else {
            activity.lifecycleScope.launch(dispatcher) {
                val summary = reminderSummaryFormatter.formatRemindersSummary(activeReminders)
                withContext(mainDispatcher) { remindersSummaryView.text = summary }
            }
        }
    }

    private fun navigateToEditFragment(medicine: Medicine) {
        val navController = findNavController(itemView)
        val action = MedicinesFragmentDirections.actionMedicinesFragmentToEditMedicineFragment(
            medicine.id
        )
        try {
            navController.navigate(action)
        } catch (_: IllegalArgumentException) {
            // Ignore
        }
    }

    fun buildTags(tagsList: List<Tag>) {
        tags.removeAllViews()
        val visibleTags = tagsList.take(MAX_VISIBLE_TAGS)
        val overflowCount = tagsList.size - visibleTags.size
        for (tag in visibleTags) {
            tags.addView(createTagChip(tag.name, isChecked = true))
        }
        if (overflowCount > 0) {
            tags.addView(createTagChip(itemView.context.getString(R.string.more_tags, overflowCount), isChecked = false))
        }
    }

    @SuppressLint("InflateParams")
    private fun createTagChip(text: String, isChecked: Boolean): Chip {
        val chip = LayoutInflater.from(itemView.context).inflate(R.layout.tag, null) as Chip
        chip.text = text
        chip.isChecked = isChecked
        chip.isCheckable = false
        chip.isCloseIconVisible = false
        chip.setOnClickListener { _: View? -> itemView.performClick() }
        chip.rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
        val margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, itemView.resources.displayMetrics).toInt()
        val params = FlexboxLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.flexShrink = 0f
        params.setMargins(margin, 0, margin, 0)
        chip.layoutParams = params
        return chip
    }

    companion object {
        private const val MAX_VISIBLE_TAGS = 5
    }
}
