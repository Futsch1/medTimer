package com.futsch1.medtimer.overview

import FilterToggleGroup
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OnFragmentReselectedListener
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.R
import com.futsch1.medtimer.overview.actions.ActionsMenu
import com.futsch1.medtimer.overview.actions.MultipleActions
import com.futsch1.medtimer.preferences.PreferencesNames.COMBINE_NOTIFICATIONS
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class OverviewFragment : Fragment(), OnFragmentReselectedListener, RemindersViewAdapter.ClickListener {

    private lateinit var adapter: RemindersViewAdapter
    private lateinit var reminders: RecyclerView
    private lateinit var medicineViewModel: MedicineViewModel
    private lateinit var optionsMenu: OptionsMenu
    private lateinit var daySelector: DaySelector
    private lateinit var overviewViewModel: OverviewViewModel
    private lateinit var fragmentOverview: FragmentSwipeLayout
    private lateinit var thread: HandlerThread
    private var onceStable = false
    private var actionMode: ActionMode? = null
    private var actionsMenu: ActionsMenu? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
        medicineViewModel.medicines.observe(this) {

        }
        overviewViewModel = ViewModelProvider(this, OverviewViewModelFactory(requireActivity().application, medicineViewModel))[OverviewViewModel::class.java]
        overviewViewModel.day = LocalDate.now()

        optionsMenu = OptionsMenu(
            this,
            medicineViewModel,
            this.findNavController(), false
        )

        thread = HandlerThread("LogManualDose")
        thread.start()

        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                actionMode?.finish()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        NextReminders(this, medicineViewModel)

        fragmentOverview = inflater.inflate(R.layout.fragment_overview, container, false) as FragmentSwipeLayout

        daySelector = DaySelector(requireContext(), fragmentOverview.findViewById(R.id.overviewWeek), overviewViewModel.day) { day -> daySelected(day) }

        requireActivity().addMenuProvider(optionsMenu, getViewLifecycleOwner())

        setupReminders()

        setupLogManualDose()
        FilterToggleGroup(fragmentOverview.findViewById(R.id.filterButtons), overviewViewModel, requireContext().getSharedPreferences("medtimer.data", 0))

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
        adapter = RemindersViewAdapter(RemindersViewAdapter.OverviewEventDiff(), requireActivity())
        reminders.setAdapter(adapter)
        reminders.setLayoutManager(LinearLayoutManager(fragmentOverview.context))
        adapter.clickListener = this

        overviewViewModel.overviewEvents.observe(getViewLifecycleOwner()) { list ->
            adapter.submitList(list) {
                reminders.post {
                    if (!onceStable && overviewViewModel.initialized) {
                        onceStable = true
                        scrollToCurrentTimeItem()
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
            val handler = Handler(thread.getLooper())
            // Run the setup of the drop down in a separate thread to access the database
            handler.post {
                ManualDose(requireContext(), medicineViewModel, this.requireActivity(), overviewViewModel.day).logManualDose()
            }
        }
    }

    fun daySelected(date: LocalDate) {
        overviewViewModel.day = date
    }


    override fun onDestroy() {
        super.onDestroy()
        if (this::thread.isInitialized) {
            thread.quit()
        }
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

        if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(COMBINE_NOTIFICATIONS, false)) {
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
            val multipleActions = MultipleActions(adapter.getSelectedItems(), this.requireActivity())
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