package com.futsch1.medtimer.overview

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.PorterDuff
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
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.ViewColorHelper
import com.futsch1.medtimer.overview.actions.createActions
import com.futsch1.medtimer.overview.actions.createActionsView
import com.google.android.material.color.MaterialColors


class ReminderViewHolder(itemView: View, val parent: ViewGroup, val fragmentActivity: FragmentActivity, val clickDelegate: ClickDelegate) :
    RecyclerView.ViewHolder(itemView) {

    val reminderText: TextView = itemView.findViewById(R.id.reminderText)
    val reminderIcon: ImageView = itemView.findViewById(R.id.reminderIcon)
    val reminderTypeIcon: ImageView = itemView.findViewById(R.id.reminderTypeIcon)
    val stateButton: ImageView = itemView.findViewById(R.id.stateButton)
    val topBar: View = itemView.findViewById(R.id.topBar)
    val bottomBar: View = itemView.findViewById(R.id.bottomBar)
    val contentContainer: View = itemView.findViewById(R.id.overviewContentContainer)
    lateinit var event: OverviewEvent
    var selected: Boolean = false

    companion object {
        fun create(parent: ViewGroup, fragmentActivity: FragmentActivity, clickDelegate: ClickDelegate): ReminderViewHolder {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.overview_item, parent, false)
            return ReminderViewHolder(view, parent, fragmentActivity, clickDelegate)
        }
    }

    fun bind(event: OverviewEvent, selected: Boolean) {
        this.event = event
        this.selected = selected

        reminderText.text = event.text
        setBarsVisibility(event.eventPosition)
        setBackgrounds()
        ViewColorHelper.setIconToImageView(contentContainer, reminderIcon, event.icon)
        reminderTypeIcon.visibility =
            if (event.reminderType == Reminder.ReminderType.REFILL || event.reminderType == Reminder.ReminderType.OUT_OF_STOCK || event.reminderType == Reminder.ReminderType.EXPIRATION_DATE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        reminderTypeIcon.setImageResource(event.reminderType.icon)
        ViewColorHelper.setDrawableTint(contentContainer, reminderTypeIcon.drawable)

        setStateButton(event.state)
        setupStateMenu()
        setupEditEvent()
    }

    private fun setBackgrounds() {
        if (event.color != null) {
            ViewColorHelper.setViewBackground(contentContainer, listOf(reminderText), event.color!!)
        } else {
            ViewColorHelper.setDefaultColors(contentContainer, listOf(reminderText))
        }
        if (selected) {
            contentContainer.backgroundTintList =
                ColorStateList.valueOf(MaterialColors.getColor(contentContainer, com.google.android.material.R.attr.colorSecondaryContainer))
            stateButton.backgroundTintList = contentContainer.backgroundTintList
        } else {
            if (event is OverviewReminderEvent && event.state != OverviewState.RAISED && event.color != null) {
                contentContainer.backgroundTintList =
                    ColorStateList.valueOf(0x20000000)
                contentContainer.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
            } else {
                contentContainer.backgroundTintList = null
                contentContainer.backgroundTintMode = null
            }
            stateButton.backgroundTintList = null
        }
    }

    private fun setupEditEvent() {
        this.contentContainer.setOnClickListener {
            if (!clickDelegate.onItemClick(layoutPosition)) {
                if (event is OverviewReminderEvent && event.state != OverviewState.RAISED) {
                    EditEventSheetDialog(fragmentActivity, (event as OverviewReminderEvent).reminderEvent)
                } else {
                    ShowMedicineSheetDialog(fragmentActivity, event.reminderId)
                }
            }
        }
    }

    private fun setupStateMenu() {
        stateButton.setOnClickListener { view ->
            popupStateMenu(view)
        }
    }

    @SuppressLint("InflateParams")
    private fun popupStateMenu(view: View) {
        val popupView: View = LayoutInflater.from(parent.context).inflate(R.layout.circular_menu_overview_event, null)
        val popupWindow = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true

        val actions = createActions(event, fragmentActivity)
        val visible = createActionsView(actions!!, popupView, popupWindow, fragmentActivity.lifecycleScope)

        if (visible) {
            // Position the view at the vertical center of the button
            val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            popupView.measure(widthMeasureSpec, heightMeasureSpec)

            val location = IntArray(2)
            view.getLocationInWindow(location)

            val popupTop = location[1] + view.height / 2 - popupView.measuredHeight / 2
            popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, location[0], popupTop)
        }
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

    fun interface ClickDelegate {
        fun onItemClick(position: Int): Boolean
    }
}