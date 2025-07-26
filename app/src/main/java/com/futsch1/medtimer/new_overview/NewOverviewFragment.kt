package com.futsch1.medtimer.new_overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.futsch1.medtimer.R
import java.time.LocalDate

class NewOverviewFragment : Fragment() {

    lateinit var daySelector: DaySelector

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_new_overview, container, false)

        daySelector = DaySelector(requireContext(), view.findViewById(R.id.overviewWeek)) { day -> daySelected(day) }

        return view
    }

    fun daySelected(date: LocalDate) {

    }
}