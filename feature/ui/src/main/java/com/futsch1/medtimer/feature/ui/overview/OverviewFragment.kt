package com.futsch1.medtimer.feature.ui.overview

import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.core.common.OnFragmentReselectedListener
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.common.helpers.EntityEditOptionsMenu
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.ui.OptionsMenuFactory
import com.futsch1.medtimer.feature.ui.R
import com.futsch1.medtimer.feature.ui.TagFilterViewModel
import com.futsch1.medtimer.feature.ui.medicine.BarcodeScanner
import com.futsch1.medtimer.feature.ui.overview.actions.ActionsFactory
import com.futsch1.medtimer.feature.ui.overview.actions.ActionsMenu
import com.futsch1.medtimer.feature.ui.overview.actions.MultipleActions
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class OverviewFragment : Fragment(), OnFragmentReselectedListener,
    RemindersViewAdapter.ClickListener {
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
    lateinit var multipleActionsFactory: MultipleActions.Factory

    @Inject
    lateinit var manualDoseFactory: ManualDose.Factory

    @Inject
    lateinit var timeFormatter: TimeFormatter

    @Inject
    lateinit var remindersViewAdapterFactory: RemindersViewAdapter.Factory

    @Inject
    lateinit var barcodeScannerFactory: BarcodeScanner.Factory
    private lateinit var barcodeScanner: BarcodeScanner

    private lateinit var adapter: RemindersViewAdapter
    private lateinit var reminders: RecyclerView
    private val tagFilterViewModel: TagFilterViewModel by activityViewModels()
    private val overviewViewModel: OverviewViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<OverviewViewModel.Factory> { factory ->
                factory.create(
                    tagFilterViewModel
                )
            }
        }
    )

    @Inject
    lateinit var optionsMenuFactory: OptionsMenuFactory
    private lateinit var optionsMenu: EntityEditOptionsMenu
    private lateinit var daySelector: DaySelector
    private var fragmentOverview: FragmentSwipeLayout? = null
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
            tagFilterViewModel
        )
        barcodeScanner = barcodeScannerFactory.create(this)

        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                actionMode?.finish()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val overview =
            inflater.inflate(R.layout.fragment_overview, container, false) as FragmentSwipeLayout
        fragmentOverview = overview

        daySelector = DaySelector(
            requireContext(),
            overview.findViewById(R.id.overviewWeek),
            overviewViewModel.day
        ) { day -> daySelected(day) }

        overview.findViewById<View>(R.id.overviewPrevWeek)
            .setOnClickListener { daySelector.scrollToPreviousWeek() }
        overview.findViewById<View>(R.id.overviewNextWeek)
            .setOnClickListener { daySelector.scrollToNextWeek() }

        viewLifecycleOwner.lifecycleScope.launch(backgroundDispatcher) {
            overviewViewModel.simulatedThrough.collect { endDay ->
                withContext(mainDispatcher) {
                    daySelector.updateRangeEnd(endDay)
                }
            }
        }

        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner())

        setupReminders(overview)

        setupLogManualDose(overview)
        setupScanBarcode(overview)
        FilterToggleGroup(
            overview.findViewById(R.id.filterButtons),
            overviewViewModel,
            persistentDataDataSource
        )

        overview.onSwipeListener = OverviewOnSwipeListener()

        return overview
    }

    override fun onResume() {
        super.onResume()
        daySelector.updateWeekRange()
        updateTitle(overviewViewModel.day)
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

    private fun setupReminders(overview: FragmentSwipeLayout) {
        reminders = overview.findViewById(R.id.reminders)
        adapter = remindersViewAdapterFactory.create(
            RemindersViewAdapter.OverviewEventDiff(),
            requireActivity()
        )
        reminders.setAdapter(adapter)
        reminders.setLayoutManager(LinearLayoutManager(overview.context))
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
            if (Instant.ofEpochSecond(listItem.timestamp).atZone(ZoneId.systemDefault())
                    .toLocalTime() >= LocalTime.now()
            ) {
                reminders.scrollToPosition(index)
                return
            }
        }
    }

    private fun setupLogManualDose(overview: FragmentSwipeLayout) {
        val logManualDose = overview.findViewById<Button>(R.id.logManualDose)
        logManualDose.setOnClickListener { _: View? ->
            lifecycleScope.launch {
                manualDoseFactory.create(
                    requireContext(),
                    overviewViewModel.medicines.value,
                    requireActivity(),
                    overviewViewModel.day
                ).logManualDose()
            }
        }
    }

    private fun setupScanBarcode(overview: FragmentSwipeLayout) {
        overview.findViewById<Button>(R.id.scanBarcode).setOnClickListener {
            barcodeScanner.scan()
        }
    }

    fun daySelected(date: LocalDate) {
        overviewViewModel.day = date
        updateTitle(date)
    }

    private fun updateTitle(date: LocalDate) {
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            getString(com.futsch1.medtimer.core.ui.R.string.tab_overview) + " - " +
                    date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        fragmentOverview = null
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
            val multipleActions =
                multipleActionsFactory.create(adapter.getSelectedItems(), requireActivity())
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
                    val button = com.futsch1.medtimer.feature.ui.overview.actions.Button.fromId(
                        item?.itemId ?: return false
                    )
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