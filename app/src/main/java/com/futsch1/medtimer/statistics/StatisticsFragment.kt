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
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.overview.EditEventSheetDialog
import com.futsch1.medtimer.statistics.ui.StatisticsScreen
import com.futsch1.medtimer.statistics.ui.StatisticsScreenViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    private val viewModel: StatisticsScreenViewModel by viewModels()

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

        composeView.setContent {
            MedTimerTheme {
                StatisticsScreen(
                    viewModel = viewModel,
                    // TODO: should be folded into the screen code once EditEventSheetDialog is moved to a more shareable module
                    onEditReminderEvent = { eventId ->
                        lifecycleScope.launch {
                            val reminderEvent = viewModel.getReminderEvent(eventId)
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