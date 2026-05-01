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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.helpers.ViewColorHelper
import com.futsch1.medtimer.overview.actions.Actions
import com.futsch1.medtimer.overview.actions.ActionsFactory
import com.futsch1.medtimer.overview.actions.Button
import com.futsch1.medtimer.overview.model.EventPosition
import com.futsch1.medtimer.overview.model.OverviewEvent
import com.futsch1.medtimer.overview.model.OverviewState
import com.futsch1.medtimer.overview.model.PastReminderEvent
import com.futsch1.medtimer.overview.model.getImage
import com.futsch1.medtimer.overview.model.toString
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class ReminderViewHolder(
    itemView: View,
    val parent: ViewGroup,
    val fragmentActivity: FragmentActivity,
    val clickDelegate: ClickDelegate,
    private val actionsFactory: ActionsFactory,
    private val medicineIcons: MedicineIcons
) : RecyclerView.ViewHolder(itemView) {

    val reminderText: TextView = itemView.findViewById(R.id.reminderText)
    val reminderIcon: ImageView = itemView.findViewById(R.id.reminderIcon)
    val stateButton: ImageView = itemView.findViewById(R.id.stateButton)
    val topBar: View = itemView.findViewById(R.id.topBar)
    val bottomBar: View = itemView.findViewById(R.id.bottomBar)
    val contentContainer: View = itemView.findViewById(R.id.overviewContentContainer)
    lateinit var event: OverviewEvent
    var selected: Boolean = false

    companion object {
        fun create(
            parent: ViewGroup,
            fragmentActivity: FragmentActivity,
            clickDelegate: ClickDelegate,
            actionsFactory: ActionsFactory,
            medicineIcons: MedicineIcons
        ): ReminderViewHolder {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.overview_item, parent, false)
            return ReminderViewHolder(view, parent, fragmentActivity, clickDelegate, actionsFactory, medicineIcons)
        }
    }

    fun bind(event: OverviewEvent, selected: Boolean) {
        this.event = event
        this.selected = selected

        reminderText.text = event.text
        setBarsVisibility(event.eventPosition)
        setBackgrounds()
        ViewColorHelper.setIconToImageView(medicineIcons, contentContainer, reminderIcon, event.icon)

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
            if (event is PastReminderEvent && event.state != OverviewState.RAISED && event.state != OverviewState.PENDING && event.color != null) {
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
                if (event is PastReminderEvent && event.state != OverviewState.RAISED && event.state != OverviewState.PENDING && event.state != OverviewState.LOCATION) {
                    EditEventSheetDialogFragment.newInstance(
                        (event as PastReminderEvent).reminderEvent.reminderEventId
                    ).show(fragmentActivity.supportFragmentManager, "EditEventDialog")
                } else {
                    ShowMedicineSheetDialogFragment.newInstance(event.reminderId)
                        .show(fragmentActivity.supportFragmentManager, "ShowMedicineDialog")
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

        val actions = actionsFactory.createActions(event, fragmentActivity)
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

    private fun createActionsView(actions: Actions, view: View, popupWindow: PopupWindow, coroutineScope: CoroutineScope): Boolean {
        view.setOnClickListener {
            popupWindow.dismiss()
        }

        // Depending on the number of visible buttons, set the angles of the anchors
        // 4 buttons -> 30, 70, 110, 150
        // 3 buttons -> 50, 90, 130
        // 2 buttons -> 70, 110
        // 1 button -> 90
        val visibleButtons = actions.visibleButtons
        val angleLists = mapOf(
            4 to listOf(30f, 70f, 110f, 150f),
            3 to listOf(50f, 90f, 130f),
            2 to listOf(70f, 110f),
            1 to listOf(90f)
        )
        var anglesIndex = 0
        for (button in Button.entries) {
            val buttonView = view.findViewById<View>(button.associatedId)
            buttonView.setOnClickListener {
                coroutineScope.launch {
                    actions.buttonClicked(button)
                }
                popupWindow.dismiss()
            }

            val isVisible = visibleButtons.contains(button)
            buttonView.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            if (!isVisible) {
                continue
            }

            // Set the circular layout angle for the anchor view
            val anchor = view.findViewById<View>(button.anchorId)
            val layoutParams = anchor.layoutParams as ConstraintLayout.LayoutParams
            val angles = angleLists[visibleButtons.size]!!
            layoutParams.circleAngle = angles[anglesIndex++]
            anchor.layoutParams = layoutParams
        }

        return visibleButtons.isNotEmpty()
    }

    private fun setStateButton(state: OverviewState) {
        stateButton.setImageResource(state.getImage())
        stateButton.tag = state.getImage()
        stateButton.contentDescription = state.toString(fragmentActivity)
    }

    private fun setBarsVisibility(position: EventPosition) {
        topBar.visibility = if (position == EventPosition.FIRST || position == EventPosition.ONLY) View.GONE else View.VISIBLE
        bottomBar.visibility = if (position == EventPosition.LAST || position == EventPosition.ONLY) View.GONE else View.VISIBLE
    }

    fun interface ClickDelegate {
        fun onItemClick(position: Int): Boolean
    }
}