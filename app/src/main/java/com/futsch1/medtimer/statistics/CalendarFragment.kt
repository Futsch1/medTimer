package com.futsch1.medtimer.statistics

import android.content.res.ColorStateList
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.text.util.LocalePreferences
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.futsch1.medtimer.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.nextMonth
import com.kizitonwose.calendar.core.previousMonth
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarFragment : Fragment() {
    private var calendarView: CalendarView? = null
    private var currentDayEvents: EditText? = null
    private var calendarEventsViewModel: CalendarEventsViewModel? = null
    private var currentDay: CalendarDay = CalendarDay(LocalDate.now(), DayPosition.MonthDate)
    private var dayStrings: Map<LocalDate, String>? = null
    private var startMonth: YearMonth? = null
    private var endMonth: YearMonth? = null

    private fun daySelected(data: CalendarDay) {
        calendarView?.notifyDayChanged(currentDay)
        currentDay = data
        updateCurrentDay()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragmentView: View =
            inflater.inflate(R.layout.fragment_calendar, container, false)

        val medicineCalenderArgs = CalendarFragmentArgs.fromBundle(requireArguments())

        calendarView =
            fragmentView.findViewById(R.id.medicineCalendar)

        currentDayEvents = fragmentView.findViewById(R.id.currentDayEvents)
        currentDayEvents?.focusable = View.NOT_FOCUSABLE
        calendarEventsViewModel = ViewModelProvider(this)[CalendarEventsViewModel::class.java]
        calendarEventsViewModel!!.getEventForDays(
            medicineCalenderArgs.medicineId,
            medicineCalenderArgs.pastDays,
            medicineCalenderArgs.futureDays
        )
            .observe(viewLifecycleOwner) { dayStrings: Map<LocalDate, String> ->
                this.dayStrings = dayStrings
                calendarView?.notifyCalendarChanged()
                updateCurrentDay()
            }

        setupCalendarView(medicineCalenderArgs)

        return fragmentView
    }

    private fun setupCalendarView(medicineCalenderArgs: CalendarFragmentArgs) {
        setupDayBinder()
        setupMonthBinder()

        startMonth = YearMonth.now().minusMonths(medicineCalenderArgs.pastDays / 30)
        endMonth = YearMonth.now().plusMonths(medicineCalenderArgs.futureDays / 30)

        calendarView?.setup(
            startMonth!!,
            endMonth!!,
            if (LocalePreferences.getFirstDayOfWeek() == LocalePreferences.FirstDayOfWeek.SUNDAY)
                DayOfWeek.SUNDAY else DayOfWeek.MONDAY
        )
        calendarView?.scrollToMonth(YearMonth.now())
    }

    private fun setupMonthBinder() {
        class MonthViewContainer(view: View) : ViewContainer(view) {
            val textView: TextView = view.findViewById(R.id.monthHeaderText)
            val prevButton: MaterialButton = view.findViewById(R.id.prevMonth)
            val nextButton: MaterialButton = view.findViewById(R.id.nextMonth)

            init {
                prevButton.setOnClickListener {
                    calendarView?.scrollToMonth(calendarView?.findFirstVisibleMonth()?.yearMonth!!.previousMonth)
                }
                nextButton.setOnClickListener {
                    calendarView?.scrollToMonth(calendarView?.findFirstVisibleMonth()?.yearMonth!!.nextMonth)
                }
            }
        }

        calendarView?.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View): MonthViewContainer = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                container.textView.text = data.yearMonth.format(
                    DateTimeFormatter.ofPattern(
                        "LLLL",
                        Locale.getDefault()
                    )
                )
                if (startMonth == data.yearMonth) {
                    container.prevButton.visibility = View.GONE
                }
                if (endMonth == data.yearMonth) {
                    container.nextButton.visibility = View.GONE
                }
            }
        }
    }

    private fun setupDayBinder() {
        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView: TextView = view.findViewById(R.id.calendarDayText)
            lateinit var day: CalendarDay

            init {
                textView.setOnClickListener {
                    daySelected(day)
                    calendarView?.notifyDayChanged(day)
                }
            }
        }

        calendarView?.dayBinder = object : MonthDayBinder<DayViewContainer> {
            private fun getColor(colorId: Int) = MaterialColors.getColor(
                calendarView!!,
                colorId
            )

            val selectedBackground = MaterialShapeDrawable()
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
                    requireContext(),
                    com.google.android.material.R.attr.colorSecondary,
                    ColorStateList.valueOf(com.google.android.material.R.attr.colorSecondary)
                )
            }

            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.textView.text =
                    String.format(Locale.getDefault(), "%d", data.date.dayOfMonth)
                if (dayStrings?.get(data.date)?.isNotEmpty() == true) {
                    container.textView.paintFlags =
                        container.textView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                }
                container.day = data
                if (data == currentDay) {
                    container.textView.setTextColor(selectedTextColor)
                    container.textView.background = selectedBackground
                } else {
                    container.textView.setTextColor(unselectedTextColor)
                    container.textView.setBackgroundColor(unselectedBackgroundColor)
                }
            }
        }
    }

    private fun updateCurrentDay() {
        val dayText = dayStrings?.get(currentDay.date)
        if (dayText != null) {
            currentDayEvents?.setText(dayText)
        } else {
            currentDayEvents?.setText(null)
        }
    }
}
