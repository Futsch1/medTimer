package com.futsch1.medtimer.overview

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.ViewColorHelper
import com.futsch1.medtimer.overview.actions.createActions


class ReminderViewHolder(itemView: View, val parent: ViewGroup, val fragmentActivity: FragmentActivity) : RecyclerView.ViewHolder(itemView) {

    val reminderText: TextView = itemView.findViewById(R.id.reminderText)
    val reminderIcon: ImageView = itemView.findViewById(R.id.reminderIcon)
    val reminderTypeIcon: ImageView = itemView.findViewById(R.id.reminderTypeIcon)
    val stateButton: ImageView = itemView.findViewById(R.id.stateButton)
    val topBar: View = itemView.findViewById(R.id.topBar)
    val bottomBar: View = itemView.findViewById(R.id.bottomBar)
    val contentContainer: View = itemView.findViewById(R.id.overviewContentContainer)
    lateinit var event: OverviewEvent

    companion object {
        fun create(parent: ViewGroup, fragmentActivity: FragmentActivity): ReminderViewHolder {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.overview_item, parent, false)
            return ReminderViewHolder(view, parent, fragmentActivity)
        }
    }

    fun bind(event: OverviewEvent) {
        this.event = event
        reminderText.text = event.text
        setBarsVisibility(event.eventPosition)
        if (event.color != null) {
            ViewColorHelper.setViewBackground(contentContainer, mutableListOf<TextView?>(reminderText), event.color!!)
        } else {
            ViewColorHelper.setDefaultColors(contentContainer, mutableListOf<TextView?>(reminderText))
        }
        ViewColorHelper.setIconToImageView(contentContainer, reminderIcon, event.icon)
        reminderTypeIcon.setImageResource(event.reminderType.icon)
        ViewColorHelper.setDrawableTint(contentContainer, reminderTypeIcon.drawable)

        setStateButton(event.state)
        setupStateMenu()
        setupEditEvent()
    }

    private fun setupEditEvent() {
        this.contentContainer.setOnClickListener { _ ->
            if (event is OverviewReminderEvent && event.state != OverviewState.RAISED) {
                EditEventSideSheetDialog(fragmentActivity, (event as OverviewReminderEvent).reminderEvent)
            }
        }
    }

    private fun setupStateMenu() {
        stateButton.setOnClickListener { view ->
            popupStateMenu(view)
        }
        contentContainer.setOnLongClickListener { view -> popupStateMenu(view); true }
    }

    @SuppressLint("InflateParams")
    private fun popupStateMenu(view: View) {
        val popupView: View = LayoutInflater.from(parent.context).inflate(R.layout.circular_menu_overview_event, null)
        val popupWindow = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true

        createActions(event, popupView, popupWindow, fragmentActivity.lifecycleScope)

        // Position the view at the vertical center of the button
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        popupView.measure(widthMeasureSpec, heightMeasureSpec)

        val location = IntArray(2)
        view.getLocationInWindow(location)

        val popupTop = location[1] + view.height / 2 - popupView.measuredHeight / 2
        popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, location[0], popupTop)
    }

    private fun setStateButton(state: OverviewState) {
        val imageResource = when (state) {
            OverviewState.PENDING -> R.drawable.alarm
            OverviewState.TAKEN -> R.drawable.check2_circle
            OverviewState.SKIPPED -> R.drawable.x_circle
            OverviewState.RAISED -> R.drawable.bell
        }
        stateButton.setImageResource(imageResource)
        stateButton.tag = imageResource
        stateButton.contentDescription = when (state) {
            OverviewState.PENDING -> fragmentActivity.getString(R.string.please_wait)
            OverviewState.TAKEN -> fragmentActivity.getString(R.string.taken)
            OverviewState.SKIPPED -> fragmentActivity.getString(R.string.skipped)
            OverviewState.RAISED -> fragmentActivity.getString(R.string.reminded)
        }
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