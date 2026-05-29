package com.futsch1.medtimer.feature.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.core.common.helpers.EntityEditOptionsMenu
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.MedicineViewModel
import com.futsch1.medtimer.feature.ui.OptionsMenuFactory
import com.futsch1.medtimer.feature.ui.overview.EditEventSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Hosts the Compose Statistics screen in a [ComposeView]. The Navigation graph and bottom-nav item
 * are unchanged. The only interop seam is [onEditEvent], which opens the existing
 * [EditEventSheetDialogFragment]. The toolbar options menu (which owns the tag filter the Reminder
 * Table observes via persisted `filterTags`) is preserved.
 */
@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    private val viewModel: StatisticsScreenViewModel by viewModels()
    private val calendarEventsViewModel: CalendarEventsViewModel by viewModels()
    private val medicineViewModel: MedicineViewModel by viewModels()

    @Inject
    lateinit var optionsMenuFactory: OptionsMenuFactory
    private lateinit var optionsMenu: EntityEditOptionsMenu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        optionsMenu = optionsMenuFactory.create(
            this,
            NavHostFragment.findNavController(this),
            true,
            medicineViewModel
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().addMenuProvider(optionsMenu, viewLifecycleOwner)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MedTimerTheme {
                    StatisticsScreen(
                        viewModel = viewModel,
                        calendarViewModel = calendarEventsViewModel,
                        onEditEvent = ::onEditEvent,
                    )
                }
            }
        }
    }

    private fun onEditEvent(reminderEventId: Int) {
        EditEventSheetDialogFragment.newInstance(reminderEventId)
            .show(parentFragmentManager, "EditEventDialog")
    }

    override fun onDestroy() {
        super.onDestroy()
        optionsMenu.onDestroy()
    }
}
