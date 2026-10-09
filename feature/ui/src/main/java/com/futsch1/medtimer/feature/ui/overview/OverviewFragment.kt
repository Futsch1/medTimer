package com.futsch1.medtimer.feature.ui.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.AppOptionsActions
import com.futsch1.medtimer.feature.ui.AppOptionsActionsFactory
import com.futsch1.medtimer.feature.ui.AppOptionsMenuHost
import com.futsch1.medtimer.feature.ui.AppOptionsViewModel
import com.futsch1.medtimer.feature.ui.TagFilterViewModel
import com.futsch1.medtimer.feature.ui.medicine.tags.TagsFragment
import com.futsch1.medtimer.feature.ui.overview.actions.ActionsFactory
import com.futsch1.medtimer.feature.ui.overview.actions.ActionsVisitor
import com.futsch1.medtimer.feature.ui.overview.actions.Button
import com.futsch1.medtimer.feature.ui.overview.actions.MultipleActions
import com.futsch1.medtimer.feature.ui.overview.manualDose.ManualDoseDialog
import com.futsch1.medtimer.feature.ui.overview.manualDose.ManualDoseViewModel
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import com.futsch1.medtimer.feature.ui.overview.model.OverviewState
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class OverviewFragment : Fragment() {
    @Inject
    lateinit var actionsVisitor: ActionsVisitor

    @Inject
    lateinit var appOptionsActionsFactory: AppOptionsActionsFactory

    @Inject
    lateinit var tagsFragmentFactory: TagsFragment.Factory

    private lateinit var appOptionsActions: AppOptionsActions
    private val appOptionsViewModel: AppOptionsViewModel by viewModels()

    private val tagFilterViewModel: TagFilterViewModel by activityViewModels()
    private val overviewViewModel: OverviewViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<OverviewViewModel.Factory> { factory ->
                factory.create(tagFilterViewModel)
            }
        }
    )

    private val manualDoseViewModel: ManualDoseViewModel by viewModels()

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
            val showManualDoseDialog = rememberSaveable { mutableStateOf(false) }

            MedTimerTheme {
                OverviewScreen(
                    viewModel = overviewViewModel,
                    onEventClick = ::onEventClick,
                    onAction = ::onAction,
                    onLogManualDose = {
                        showManualDoseDialog.value = true
                        manualDoseViewModel.reset()
                        manualDoseViewModel.selectDay(overviewViewModel.state.day)
                                      },
                    topBarActions = {
                        AppOptionsMenuHost(
                            fragment = this@OverviewFragment,
                            actions = appOptionsActions,
                            optionsViewModel = appOptionsViewModel,
                            tagFilterViewModel = tagFilterViewModel,
                            tagsFragmentFactory = tagsFragmentFactory,
                        )
                    },
                )

                if (showManualDoseDialog.value) {
                    ManualDoseDialog(
                        viewModel = manualDoseViewModel,
                        onSearch = {
                            manualDoseViewModel.search(it)
                        },
                        onSelectMedicineEntry = {
                            manualDoseViewModel.selectMedicineEntry(it)
                        },
                        onSelectAmount = {
                            manualDoseViewModel.selectAmount(it)
                        },
                        onLogManualDose = {
                            manualDoseViewModel.selectTimeAndLog(it)
                        },
                        onStepBack = {
                            manualDoseViewModel.goOneStepBack()
                        },
                        onDismissDialog = {
                            showManualDoseDialog.value = false
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The battery exemption may have been granted in system settings while we were away.
        overviewViewModel.refreshWarnings()
        if (overviewViewModel.initializedDay != LocalDate.now()) {
            jumpToToday()
            overviewViewModel.initializedDay = LocalDate.now()
        }
    }

    fun jumpToToday() {
        overviewViewModel.selectDay(LocalDate.now())
    }

    override fun onDestroy() {
        super.onDestroy()
        appOptionsActions.onDestroy()
    }

    private fun onEventClick(event: OverviewEvent) {
        val editable = event is PastReminderEvent &&
                event.state != OverviewState.RAISED &&
                event.state != OverviewState.PENDING &&
                event.state != OverviewState.LOCATION
        if (editable) {
            EditEventSheetDialogFragment
                .newInstance(event.reminderEvent.reminderEventId)
                .show(parentFragmentManager, "EditEventDialog")
        } else {
            ShowMedicineSheetDialogFragment.newInstance(event.reminderId)
                .show(parentFragmentManager, "ShowMedicineDialog")
        }
    }

    private fun onAction(button: Button, events: List<OverviewEvent>) {
        val actions = if (events.size == 1) {
            ActionsFactory().createActions(events.first())
        } else {
            MultipleActions(events)
        } ?: return
        lifecycleScope.launch {
            actionsVisitor.startVisit(button).use {
                actions.buttonClicked(actionsVisitor)
            }
        }
    }
}
