package com.futsch1.medtimer.new_overview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.helpers.ViewColorHelper
import com.futsch1.medtimer.new_overview.ManualDose.ManualDoseEntry

class ManualDoseListEntryAdapter(context: Context, val resource: Int, entries: List<ManualDoseEntry>) :
    ArrayAdapter<ManualDoseEntry>(context, resource, entries) {
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
            val iconDrawable = MedicineIcons(context).getIconDrawable(currentItem.iconId)
            ViewColorHelper.setDrawableTint(textView, iconDrawable)
            iconDrawable
        } else null

        textView.setCompoundDrawablesWithIntrinsicBounds(iconDrawable, null, null, null)

        return view
    }
}