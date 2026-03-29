package com.futsch1.medtimer.overview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.helpers.ViewColorHelper
import com.futsch1.medtimer.overview.ManualDose.ManualDoseEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ManualDoseListEntryAdapter @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted val resource: Int,
    @Assisted entries: List<ManualDoseEntry>,
    private val medicineIcons: MedicineIcons
) : ArrayAdapter<ManualDoseEntry>(context, resource, entries) {

    @AssistedFactory
    interface Factory {
        fun create(context: Context, resource: Int, entries: List<ManualDoseEntry>): ManualDoseListEntryAdapter
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)

        val currentItem = getItem(position)!!

        val textView: TextView = view.findViewById(R.id.entry_text)

        textView.text = currentItem.name
        if (currentItem.useColor) {
            ViewColorHelper.setViewBackground(textView, listOf(textView), currentItem.color)
        } else {
            ViewColorHelper.setDefaultColors(textView, listOf(textView))
        }

        val iconDrawable = if (currentItem.iconId != 0) {
            val iconDrawable = medicineIcons.getIconDrawable(currentItem.iconId)
            ViewColorHelper.setDrawableTint(textView, iconDrawable)
            iconDrawable
        } else null

        textView.setCompoundDrawablesWithIntrinsicBounds(iconDrawable, null, null, null)

        return view
    }
}