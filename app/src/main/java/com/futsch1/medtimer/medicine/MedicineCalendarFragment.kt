package com.futsch1.medtimer.medicine

import android.content.res.ColorStateList
import android.graphics.Typeface
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
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MedicineCalendarFragment : Fragment() {
    private var calendarView: CalendarView? = null
    private var currentDayEvents: EditText? = null
    private var medicineEventsViewModel: MedicineEventsViewModel? = null
    private var currentDay: CalendarDay? = null
    private var dayStrings: Map<LocalDate, String>? = null

    private fun daySelected(data: CalendarDay) {
        if (currentDay != null) {
            calendarView?.notifyDayChanged(currentDay!!)
        }
        currentDay = data
        updateCurrentDay()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragmentView: View =
            inflater.inflate(R.layout.fragment_medicine_calendar, container, false)

        val medicineCalenderArgs = MedicineCalendarFragmentArgs.fromBundle(requireArguments())

        calendarView =
            fragmentView.findViewById(R.id.medicineCalendar)

        currentDayEvents = fragmentView.findViewById(R.id.currentDayEvents)
        medicineEventsViewModel = ViewModelProvider(this)[MedicineEventsViewModel::class.java]
        medicineEventsViewModel!!.getEventForDays(medicineCalenderArgs.medicineId, 30)
            .observe(viewLifecycleOwner) { dayStrings: Map<LocalDate, String> ->
                this.dayStrings = dayStrings
                calendarView?.notifyCalendarChanged()
                updateCurrentDay()
            }

        setupCalendarView()


        return fragmentView
    }

    private fun setupCalendarView() {
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
            val selectedBackground = MaterialShapeDrawable()
            val selectedTextColor = MaterialColors.getColor(
                calendarView!!,
                com.google.android.material.R.attr.colorOnSecondary
            )
            val unselectedTextColor = MaterialColors.getColor(
                calendarView!!,
                com.google.android.material.R.attr.colorOnSurface
            )
            val unselectedBackgroundColor = MaterialColors.getColor(
                calendarView!!,
                com.google.android.material.R.attr.colorSurface
            )

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
                container.textView.text = data.date.dayOfMonth.toString()
                if (dayStrings?.get(data.date)?.isNotEmpty() == true) {
                    container.textView.setTypeface(null, Typeface.BOLD)
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

        calendarView?.setup(
            YearMonth.now().minusMonths(1),
            YearMonth.now().plusMonths(1),
            if (LocalePreferences.getFirstDayOfWeek() == LocalePreferences.FirstDayOfWeek.SUNDAY)
                DayOfWeek.SUNDAY else DayOfWeek.MONDAY
        )
        calendarView?.scrollToMonth(YearMonth.now())
    }

    private fun updateCurrentDay() {
        dayStrings?.get(currentDay?.date)?.let { currentDayEvents?.setText(it) }
    }
}
