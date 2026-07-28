package com.futsch1.medtimer.feature.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.futsch1.medtimer.core.ui.ScreenTestTags
import com.futsch1.medtimer.core.ui.component.MedTimerTopAppBar
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.AppOptionsActions
import com.futsch1.medtimer.feature.ui.AppOptionsActionsFactory
import com.futsch1.medtimer.feature.ui.AppOptionsMenuHost
import com.futsch1.medtimer.feature.ui.AppOptionsViewModel
import com.futsch1.medtimer.feature.ui.TagFilterViewModel
import com.futsch1.medtimer.feature.ui.medicine.tags.TagsFragment
import com.futsch1.medtimer.feature.ui.overview.EditEventSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.futsch1.medtimer.core.ui.R as CoreUiR

// The tag filter persists filterTags; StatisticsScreenViewModel reads them via PersistentDataDataSource.
@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    private val viewModel: StatisticsScreenViewModel by viewModels()
    private val tagFilterViewModel: TagFilterViewModel by viewModels()
    private val appOptionsViewModel: AppOptionsViewModel by viewModels()

    @Inject
    lateinit var appOptionsActionsFactory: AppOptionsActionsFactory

    @Inject
    lateinit var tagsFragmentFactory: TagsFragment.Factory

    private lateinit var appOptionsActions: AppOptionsActions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appOptionsActions = appOptionsActionsFactory.create(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MedTimerTheme {
                Column(Modifier.testTag(ScreenTestTags.STATISTICS)) {
                    MedTimerTopAppBar(
                        title = stringResource(CoreUiR.string.analysis),
                        actions = {
                            AppOptionsMenuHost(
                                fragment = this@StatisticsFragment,
                                actions = appOptionsActions,
                                optionsViewModel = appOptionsViewModel,
                                tagFilterViewModel = tagFilterViewModel,
                                tagsFragmentFactory = tagsFragmentFactory,
                                showTagFilter = false,
                            )
                        },
                    )
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
        appOptionsActions.onDestroy()
    }
}
