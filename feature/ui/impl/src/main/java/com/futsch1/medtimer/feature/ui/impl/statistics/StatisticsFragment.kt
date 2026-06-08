package com.futsch1.medtimer.feature.ui.impl.statistics

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
import com.futsch1.medtimer.feature.ui.impl.OptionsMenuFactory
import com.futsch1.medtimer.feature.ui.impl.TagFilterViewModel
import com.futsch1.medtimer.feature.ui.impl.overview.EditEventSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// The options menu persists filterTags; StatisticsScreenViewModel reads them via PersistentDataDataSource.
@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    private val viewModel: StatisticsScreenViewModel by viewModels()
    private val tagFilterViewModel: TagFilterViewModel by viewModels()

    @Inject
    lateinit var optionsMenuFactory: OptionsMenuFactory
    private lateinit var optionsMenu: EntityEditOptionsMenu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        optionsMenu = optionsMenuFactory.create(
            this,
            NavHostFragment.findNavController(this),
            true,
            tagFilterViewModel
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
