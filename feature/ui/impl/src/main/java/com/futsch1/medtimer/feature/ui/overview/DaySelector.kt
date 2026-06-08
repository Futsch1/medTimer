package com.futsch1.medtimer.feature.ui.overview

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import com.futsch1.medtimer.feature.ui.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.WeekDayPosition
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekCalendarView
import com.kizitonwose.calendar.view.WeekDayBinder
import com.futsch1.medtimer.feature.reminders.api.SimulatedReminders
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class DaySelector(
    val context: Context,
    val calendarView: WeekCalendarView,
    var startDay: LocalDate,
    val daySelected: (LocalDate) -> Unit
) {
    private var currentDay: WeekDay = WeekDay(startDay, WeekDayPosition.RangeDate)
    private val rangeStartDay: LocalDate = LocalDate.now().minusYears(3)
    var rangeEndDay: LocalDate = LocalDate.now().plusDays(SimulatedReminders.DEFAULT_SIMULATION_DAYS)

    init {
        setupDayBinder()
        // firstDayOfWeek = today - 3 days so today is always at position 4 of 7 on first open
        calendarView.setup(rangeStartDay, rangeEndDay, LocalDate.now().minusDays(3).dayOfWeek)
        // Scroll synchronously before the listener is registered so the initial posted
        // notifyWeekScrollListenerIfNeeded fires while today's week is visible, not week-1.
        calendarView.scrollToDate(startDay)
        setupWeekScrollListener()
    }

    private fun setupWeekScrollListener() {
        calendarView.weekScrollListener = { week ->
            val weekDates = week.days.map { it.date }
            val today = LocalDate.now()
            // Only auto-select when current selection is not in the newly visible week — prevents
            // feedback loops from programmatic scrollToWeek calls and initial setup fires.
            if (!weekDates.contains(currentDay.date)) {
                val newDay = when {
                    weekDates.contains(today) -> today
                    weekDates.last() < currentDay.date -> weekDates.last()  // scrolled back
                    else -> weekDates.first()  // scrolled forward
                }
                notifyDaySelected(WeekDay(newDay, WeekDayPosition.RangeDate))
                calendarView.notifyDayChanged(WeekDay(newDay, WeekDayPosition.RangeDate))
            }
        }
    }

    fun setDay(date: LocalDate) {
        notifyDaySelected(WeekDay(date, WeekDayPosition.RangeDate))
    }

    private fun notifyDaySelected(data: WeekDay) {
        calendarView.notifyCalendarChanged()
        currentDay = data
        daySelected(currentDay.date)
        calendarView.scrollToWeek(currentDay.date)
    }

    private fun setupDayBinder() {
        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView: TextView = view.findViewById(R.id.calendarDayText)
            lateinit var day: WeekDay

            init {
                view.setOnClickListener {
                    notifyDaySelected(day)
                    calendarView.notifyDayChanged(day)
                }
            }
        }

        calendarView.dayBinder = object : WeekDayBinder<DayViewContainer> {
            private fun getColor(colorId: Int) = MaterialColors.getColor(
                calendarView,
                colorId
            )

            val selectedBackground = MaterialShapeDrawable()
            val todayBackground = MaterialShapeDrawable()
            val todayTextColor = getColor(com.google.android.material.R.attr.colorOnPrimaryContainer)
            val selectedTextColor = getColor(com.google.android.material.R.attr.colorOnSecondary)
            val unselectedTextColor = getColor(com.google.android.material.R.attr.colorOnSurface)
            val unselectedBackgroundColor =
                getColor(com.google.android.material.R.attr.colorSurface)

            init {
                selectedBackground.shapeAppearanceModel = ShapeAppearanceModel.builder(
                    context,
                    com.google.android.material.R.style.ShapeAppearance_MaterialComponents_SmallComponent,
                    com.google.android.material.R.style.ShapeAppearanceOverlay_MaterialComponents_MaterialCalendar_Day
                ).build()
                selectedBackground.fillColor = MaterialColors.getColorStateList(
                    context,
                    com.google.android.material.R.attr.colorSecondary,
                    ColorStateList.valueOf(com.google.android.material.R.attr.colorSecondary)
                )

                todayBackground.shapeAppearanceModel = selectedBackground.shapeAppearanceModel
                todayBackground.fillColor = MaterialColors.getColorStateList(
                    context,
                    com.google.android.material.R.attr.colorPrimaryContainer,
                    ColorStateList.valueOf(com.google.android.material.R.attr.colorPrimaryContainer)
                )
            }

            override fun create(view: View) = DayViewContainer(view)

            @SuppressLint("SetTextI18n")
            override fun bind(container: DayViewContainer, data: WeekDay) {
                container.textView.text = "${data.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}\n${
                    String.format(
                        Locale.getDefault(),
                        "%d",
                        data.date.dayOfMonth
                    )
                }"
                container.day = data
                if (data == currentDay) {
                    container.textView.setTextColor(selectedTextColor)
                    container.textView.background = selectedBackground
                    container.textView.tag = "selected"
                } else {
                    container.textView.tag = null
                    if (data.date == LocalDate.now()) {
                        container.textView.setTextColor(todayTextColor)
                        container.textView.background = todayBackground
                    } else {
                        container.textView.setTextColor(unselectedTextColor)
                        container.textView.setBackgroundColor(unselectedBackgroundColor)
                    }

                }
            }
        }
    }

    fun selectPreviousDay() {
        if (currentDay.date.minusDays(1) >= rangeStartDay) {
            notifyDaySelected(WeekDay(currentDay.date.minusDays(1), WeekDayPosition.RangeDate))
        }
    }

    fun selectNextDay() {
        if (currentDay.date.plusDays(1) <= rangeEndDay) {
            notifyDaySelected(WeekDay(currentDay.date.plusDays(1), WeekDayPosition.RangeDate))
        }
    }

    fun scrollToPreviousWeek() {
        val target = currentDay.date.minusWeeks(1)
        if (target >= rangeStartDay) {
            calendarView.smoothScrollToWeek(target)
        }
    }

    fun scrollToNextWeek() {
        val target = currentDay.date.plusWeeks(1)
        if (target <= rangeEndDay) {
            calendarView.smoothScrollToWeek(target)
        }
    }

    fun updateRangeEnd(newEndDay: LocalDate) {
        if (newEndDay > rangeEndDay) {
            rangeEndDay = newEndDay
            calendarView.updateWeekData(endDate = rangeEndDay)
        }
    }

    fun updateWeekRange() {
        // Reset to today when the date has rolled over since the selector was last active
        val today = LocalDate.now()
        if (startDay < today) {
            startDay = today
            // Reconfigure firstDayOfWeek so today stays at position 4 in the new day
            calendarView.updateWeekData(firstDayOfWeek = today.minusDays(3).dayOfWeek)
            notifyDaySelected(WeekDay(today, WeekDayPosition.RangeDate))
        }
    }
}

