package com.futsch1.medtimer.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.statistics.ui.calendar.CalendarContent
import com.futsch1.medtimer.statistics.ui.calendar.CalendarEventsViewModel

class CalendarFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext())
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        val args = if (arguments != null) {
            CalendarFragmentArgs.fromBundle(requireArguments())
        } else {
            CalendarFragmentArgs.Builder(-1, 3, 0).build()
        }

        val calendarViewModel = ViewModelProvider(this)[CalendarEventsViewModel::class.java]

        composeView.setContent {
            MedTimerTheme {
                CalendarContent(
                    viewModel = calendarViewModel,
                    medicineId = args.medicineId,
                    pastMonths = args.pastMonths,
                    futureMonths = args.futureMonths,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        return composeView
    }
}
