package com.futsch1.medtimer.medicine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.futsch1.medtimer.R
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MedicineCalendarFragment : Fragment() {
    private var currentDayEvents: EditText? = null
    private var medicineEventsViewModel: MedicineEventsViewModel? = null
    private var currentDay: LocalDate? = null
    private var dayStrings: Map<LocalDate, String>? = null
    private fun daySelected(data: CalendarDay) {
        currentDay = data.date
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

        val calendarView =
            fragmentView.findViewById<com.kizitonwose.calendar.view.CalendarView>(R.id.medicineCalendar)
        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.bind(data.date.dayOfMonth.toString()) { daySelected(data) }
            }
        }
        calendarView.setup(YearMonth.now(), YearMonth.now().plusMonths(6), DayOfWeek.SUNDAY)
        calendarView.scrollToMonth(YearMonth.now())

        return fragmentView
    }

    private fun updateCurrentDay() {
        dayStrings?.get(currentDay)?.let { currentDayEvents?.setText(it) }
    }
}

class DayViewContainer(view: View) : ViewContainer(view) {
    private val textView: TextView = view.findViewById(R.id.calendarDayText)

    fun bind(dayText: String, clicked: () -> Unit) {
        textView.text = dayText
        textView.setOnClickListener { _: View -> clicked() }
    }
}