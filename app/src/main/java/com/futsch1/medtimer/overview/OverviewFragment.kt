package com.futsch1.medtimer.overview

import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OnFragmentReselectedListener
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.R
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.overview.actions.ActionsFactory
import com.futsch1.medtimer.overview.actions.ActionsMenu
import com.futsch1.medtimer.overview.actions.MultipleActions
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.preferences.PreferencesDataSource

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class OverviewFragment : Fragment(), OnFragmentReselectedListener, RemindersViewAdapter.ClickListener {
    @Inject
    @Dispatcher(MedTimerDispatchers.Default)
    lateinit var backgroundDispatcher: CoroutineDispatcher

    @Inject
    @Dispatcher(MedTimerDispatchers.Main)
    lateinit var mainDispatcher: CoroutineDispatcher

    // TODO: Remove these data sources again (as they should be part of view models) when the dependent classes are DId
    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    @Inject
    lateinit var persistentDataDataSource: PersistentDataDataSource

    @Inject
    lateinit var actionsFactory: ActionsFactory

    @Inject
    lateinit var manualDoseFactory: ManualDose.Factory

    private lateinit var adapter: RemindersViewAdapter
    private lateinit var reminders: RecyclerView
    private val medicineViewModel: MedicineViewModel by viewModels()
    private val overviewViewModel: OverviewViewModel by viewModels {
        OverviewViewModelFactory(
            requireActivity().application,
            preferencesDataSource,
            medicineViewModel
        )
    }

    @Inject
    lateinit var optionsMenuFactory: OptionsMenu.Factory
    private lateinit var optionsMenu: OptionsMenu
    private lateinit var daySelector: DaySelector
    private lateinit var fragmentOverview: FragmentSwipeLayout
    private var onceStable = false
    private var actionMode: ActionMode? = null
    private var actionsMenu: ActionsMenu? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overviewViewModel.day = LocalDate.now()

        optionsMenu = optionsMenuFactory.create(
            this,
            this.findNavController(),
            false,
            medicineViewModel
        )

        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                actionMode?.finish()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        NextReminders(this, medicineViewModel, preferencesDataSource)

        fragmentOverview = inflater.inflate(R.layout.fragment_overview, container, false) as FragmentSwipeLayout

        daySelector = DaySelector(requireContext(), fragmentOverview.findViewById(R.id.overviewWeek), overviewViewModel.day) { day -> daySelected(day) }

        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner())

        setupReminders()

        setupLogManualDose()
        FilterToggleGroup(fragmentOverview.findViewById(R.id.filterButtons), overviewViewModel, persistentDataDataSource)

        fragmentOverview.onSwipeListener = OverviewOnSwipeListener()

        return fragmentOverview
    }

    inner class OverviewOnSwipeListener : OnSwipeListener {
        override fun onSwipeLeft() {
            daySelector.selectNextDay()
        }

        override fun onSwipeRight() {
            daySelector.selectPreviousDay()
        }

        override fun onSwipeUp() {
            // Not required
        }

        override fun onSwipeDown() {
            // Not required
        }
    }

    private fun setupReminders() {
        reminders = fragmentOverview.findViewById(R.id.reminders)
        adapter = RemindersViewAdapter(RemindersViewAdapter.OverviewEventDiff(), requireActivity(), preferencesDataSource, actionsFactory)
        reminders.setAdapter(adapter)
        reminders.setLayoutManager(LinearLayoutManager(fragmentOverview.context))
        adapter.clickListener = this

        viewLifecycleOwner.lifecycleScope.launch(backgroundDispatcher) {
            overviewViewModel.overviewEvents.collect { list ->
                withContext(mainDispatcher) {
                    adapter.submitList(list) {
                        reminders.post {
                            if (onceStable || !overviewViewModel.initialized) {
                                return@post
                            }

                            onceStable = true
                            scrollToCurrentTimeItem()
                        }
                    }
                }
            }
        }
    }

    private fun scrollToCurrentTimeItem() {
        adapter.currentList.forEachIndexed { index, listItem ->
            if (Instant.ofEpochSecond(listItem.timestamp).atZone(ZoneId.systemDefault()).toLocalTime() >= LocalTime.now()) {
                reminders.scrollToPosition(index)
                return
            }
        }
    }

    private fun setupLogManualDose() {
        val logManualDose = fragmentOverview.findViewById<Button>(R.id.logManualDose)
        logManualDose.setOnClickListener { _: View? ->
            lifecycleScope.launch {
                manualDoseFactory.create(requireContext(), medicineViewModel, requireActivity(), overviewViewModel.day).logManualDose()
            }
        }
    }

    fun daySelected(date: LocalDate) {
        overviewViewModel.day = date
    }


    override fun onDestroy() {
        super.onDestroy()
        if (this::optionsMenu.isInitialized) {
            optionsMenu.onDestroy()
        }
    }

    override fun onFragmentReselected() {
        daySelector.setDay(LocalDate.now())
    }

    override fun onItemClick(position: Int) {
        if (actionMode != null) {
            adapter.toggleSelection(position)
            updateActionMode()
        }
    }

    override fun onItemLongClick(position: Int) {
        if (actionMode == null) {
            actionMode = requireActivity().startActionMode(ActionModeCallback())
            adapter.selectionMode = true
            onBackPressedCallback.isEnabled = true
        }

        if (preferencesDataSource.preferences.value.combineNotifications) {
            adapter.selectSameTimeEvents(position)
        } else {
            adapter.toggleSelection(position)
        }
        updateActionMode()
    }

    fun updateActionMode() {
        val selectedCount = adapter.getSelectedCount()
        if (selectedCount == 0) {
            actionMode?.finish()
            actionsMenu = null
        } else {
            actionMode?.title = selectedCount.toString()
            val multipleActions = MultipleActions(actionsFactory, adapter.getSelectedItems())
            actionsMenu = ActionsMenu(actionMode!!.menu, multipleActions)
        }

    }

    inner class ActionModeCallback : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.overview_multi_selection, menu)
            mode?.title = "1"
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (actionsMenu != null) {
                if (item?.itemId == R.id.selectAll) {
                    adapter.selectAll()
                    updateActionMode()
                } else {
                    val button = com.futsch1.medtimer.overview.actions.Button.fromId(item?.itemId ?: return false)
                    lifecycleScope.launch {
                        actionsMenu?.actions?.buttonClicked(button)
                        mode?.finish()
                    }
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            adapter.selectionMode = false
            actionMode = null
            onBackPressedCallback.isEnabled = false
        }
    }
}