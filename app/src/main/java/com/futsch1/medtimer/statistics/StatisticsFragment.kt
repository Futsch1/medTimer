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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.statusValuesWithoutDeletedAndAcknowledged
import com.futsch1.medtimer.overview.EditEventSheetDialog
import com.futsch1.medtimer.statistics.ui.StatisticsScreen
import com.futsch1.medtimer.statistics.ui.StatisticsScreenViewModel
import com.futsch1.medtimer.statistics.ui.calendar.CalendarEventsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatisticsFragment : Fragment() {
    private var optionsMenu: OptionsMenu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        optionsMenu = OptionsMenu(
            this,
            ViewModelProvider(this)[MedicineViewModel::class.java],
            NavHostFragment.findNavController(this), true
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext())
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        val viewModel = ViewModelProvider(this)[StatisticsScreenViewModel::class.java]
        val calendarViewModel = ViewModelProvider(this)[CalendarEventsViewModel::class.java]
        val medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
        val medicineRepository = MedicineRepository(requireActivity().application)

        val reminderEvents =
            medicineViewModel.getLiveReminderEvents(0, statusValuesWithoutDeletedAndAcknowledged)
        composeView.setContent {
            MedTimerTheme {
                StatisticsScreen(
                    viewModel = viewModel,
                    calendarViewModel = calendarViewModel,
                    reminderEvents = reminderEvents,
                    onEditReminderEvent = { eventId ->
                        lifecycleScope.launch {
                            val reminderEvent = withContext(Dispatchers.IO) {
                                medicineRepository.getReminderEvent(eventId)
                            }
                            if (reminderEvent != null) {
                                EditEventSheetDialog(
                                    requireActivity(),
                                    reminderEvent
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                )
            }
        }

        requireActivity().addMenuProvider(optionsMenu!!, viewLifecycleOwner)

        return composeView
    }

    override fun onDestroy() {
        super.onDestroy()
        optionsMenu?.onDestroy()
    }
}
