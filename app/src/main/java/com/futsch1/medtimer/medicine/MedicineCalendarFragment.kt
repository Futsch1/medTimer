package com.futsch1.medtimer.medicine

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
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

        currentDayEvents = fragmentView.findViewById(R.id.currentDayEvents)
        medicineEventsViewModel = ViewModelProvider(this)[MedicineEventsViewModel::class.java]
        medicineEventsViewModel!!.getEventForDays(medicineCalenderArgs.medicineId, 30)
            .observe(viewLifecycleOwner) { dayStrings: Map<LocalDate, String> ->
                this.dayStrings = dayStrings
                updateCurrentDay()
            }

        calendarView =
            fragmentView.findViewById(R.id.medicineCalendar)

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
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.textView.text = data.date.dayOfMonth.toString()
                container.day = data
                if (data == currentDay) {
                    container.textView.setTextColor(
                        MaterialColors.getColor(
                            container.textView,
                            com.google.android.material.R.attr.colorOnSecondary
                        )
                    )
                    val shape = MaterialShapeDrawable()
                    shape.shapeAppearanceModel = ShapeAppearanceModel.builder(
                        context,
                        com.google.android.material.R.style.ShapeAppearance_MaterialComponents_SmallComponent,
                        com.google.android.material.R.style.ShapeAppearanceOverlay_MaterialComponents_MaterialCalendar_Day
                    ).build()
                    shape.fillColor = MaterialColors.getColorStateList(
                        requireContext(),
                        com.google.android.material.R.attr.colorSecondary,
                        ColorStateList.valueOf(com.google.android.material.R.attr.colorSecondary)
                    )
                    container.textView.background = shape
                } else {
                    container.textView.setTextColor(
                        MaterialColors.getColor(
                            container.textView,
                            com.google.android.material.R.attr.colorOnSurface
                        )
                    )
                    container.textView.setBackgroundColor(
                        MaterialColors.getColor(
                            container.textView,
                            com.google.android.material.R.attr.colorSurface
                        )
                    )
                }
            }
        }
        calendarView?.setup(YearMonth.now(), YearMonth.now().plusMonths(6), DayOfWeek.SUNDAY)
        calendarView?.scrollToMonth(YearMonth.now())
    }

    private fun updateCurrentDay() {
        dayStrings?.get(currentDay?.date)?.let { currentDayEvents?.setText(it) }
    }
}
