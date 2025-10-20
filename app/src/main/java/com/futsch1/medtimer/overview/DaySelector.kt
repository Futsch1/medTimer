package com.futsch1.medtimer.overview

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import com.futsch1.medtimer.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.WeekDayPosition
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekCalendarView
import com.kizitonwose.calendar.view.WeekDayBinder
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class DaySelector(
    val context: Context,
    val calendarView: WeekCalendarView,
    startDay: LocalDate,
    val daySelected: (LocalDate) -> Unit
) {
    private var currentDay: WeekDay = WeekDay(startDay, WeekDayPosition.RangeDate)
    val startDate: LocalDate = LocalDate.now().minusDays(5)
    val endDate: LocalDate = LocalDate.now().plusDays(1)

    init {
        setupDayBinder()
        calendarView.setup(
            startDate,
            endDate,
            startDate.dayOfWeek
        )
    }

    fun setDay(date: LocalDate) {
        notifyDaySelected(WeekDay(date, WeekDayPosition.RangeDate))
    }

    private fun notifyDaySelected(data: WeekDay) {
        calendarView.notifyCalendarChanged()
        currentDay = data
        daySelected(currentDay.date)
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
        if (currentDay.date.minusDays(1) >= startDate) {
            notifyDaySelected(WeekDay(currentDay.date.minusDays(1), WeekDayPosition.RangeDate))
        }
    }

    fun selectNextDay() {
        if (currentDay.date.plusDays(1) <= endDate) {
            notifyDaySelected(WeekDay(currentDay.date.plusDays(1), WeekDayPosition.RangeDate))
        }
    }
}