package com.futsch1.medtimer.new_overview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.ViewColorHelper

enum class EventPosition {
    FIRST,

    @Suppress("unused")
    MIDDLE,
    LAST,
    ONLY
}

class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val reminderText: TextView = itemView.findViewById(R.id.reminderText)
    val reminderIcon: ImageView = itemView.findViewById(R.id.reminderIcon)
    val stateButton: ImageView = itemView.findViewById(R.id.stateButton)
    val topBar: View = itemView.findViewById(R.id.topBar)
    val bottomBar: View = itemView.findViewById(R.id.bottomBar)

    companion object {
        fun create(parent: ViewGroup): ReminderViewHolder {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.overview_item, parent, false)
            return ReminderViewHolder(view)
        }
    }

    fun bind(event: OverviewEvent, position: EventPosition) {
        reminderText.text = event.text
        if (event.color != null) {
            ViewColorHelper.setViewBackground(itemView, mutableListOf<TextView?>(reminderText), event.color)
        } else {
            ViewColorHelper.setDefaultColors(itemView, mutableListOf<TextView?>(reminderText))
        }
        ViewColorHelper.setIconToImageView(itemView, reminderIcon, event.icon)

        setBarsVisibility(position)
        setStateButton(event.state)
    }

    private fun setStateButton(state: OverviewState) {
        val imageResource = when (state) {
            OverviewState.PENDING -> R.drawable.alarm
            OverviewState.TAKEN -> R.drawable.check2_circle
            OverviewState.SKIPPED -> R.drawable.x_circle
            OverviewState.RAISED -> R.drawable.bell
        }
        stateButton.setImageResource(imageResource)
    }

    private fun setBarsVisibility(position: EventPosition) {
        if (position == EventPosition.FIRST) {
            topBar.visibility = View.GONE
        } else {
            topBar.visibility = View.VISIBLE
        }

        if (position == EventPosition.LAST) {
            bottomBar.visibility = View.GONE
        } else {
            bottomBar.visibility = View.VISIBLE
        }

        if (position == EventPosition.ONLY) {
            topBar.visibility = View.GONE
            bottomBar.visibility = View.GONE
        }
    }
}