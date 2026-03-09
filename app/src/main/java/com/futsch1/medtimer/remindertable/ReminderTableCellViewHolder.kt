package com.futsch1.medtimer.remindertable

import android.graphics.Paint
import android.view.View
import android.widget.TextView
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.getMaterialColor

class ReminderTableCellViewHolder(view: View) : AbstractViewHolder(view) {
    val textView: TextView = view.findViewById(R.id.tableCellTextView)

    init {
        textView.setTextColor(
            view.context.getMaterialColor(
                com.google.android.material.R.attr.colorOnSecondaryContainer,
                "TableView"
            )
        )
        textView.isClickable = false
        textView.paintFlags = textView.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
    }

    fun setupEditButton(clickListener: OnEditClickListener?) {
        if (clickListener == null) {
            textView.isClickable = false
            textView.paintFlags = textView.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
            return
        }

        textView.setOnClickListener { clickListener.onEditClick() }
        textView.paintFlags = textView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }

    interface OnEditClickListener {
        fun onEditClick()
    }
}
