package com.futsch1.medtimer.remindertable

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.evrencoskun.tableview.TableView
import com.evrencoskun.tableview.filter.Filter
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.statusValuesWithoutDeletedAndAcknowledged
import com.futsch1.medtimer.helpers.TableHelper
import com.futsch1.medtimer.helpers.getMaterialColor
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReminderTableFragment : Fragment() {
    private lateinit var filterLayout: TextInputLayout
    private lateinit var filter: TextInputEditText

    private val medicineViewModel: MedicineViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragmentView = inflater.inflate(R.layout.fragment_reminder_table, container, false)
        val tableView = fragmentView.findViewById<TableView>(R.id.reminder_table)
        filter = fragmentView.findViewById(R.id.filter)
        filterLayout = fragmentView.findViewById(R.id.filterLayout)
        val tableFilter = Filter(tableView)
        setupFilter(tableFilter)

        val adapter = ReminderTableAdapter(tableView, medicineViewModel, requireActivity())

        tableView.setAdapter(adapter)
        adapter.setColumnHeaderItems(TableHelper.getTableHeadersForAnalysis(requireContext()))
        viewLifecycleOwner.lifecycleScope.launch {
            medicineViewModel.getLiveReminderEvents(0, statusValuesWithoutDeletedAndAcknowledged)
                .collect { reminderEvents ->
                    adapter.submitList(reminderEvents)
                    tableFilter.set(filter.text.toString())
                }
        }

        // This is a workaround for a recycler view bug that causes random crashes
        tableView.cellRecyclerView.setItemAnimator(null)
        tableView.columnHeaderRecyclerView.setItemAnimator(null)
        tableView.rowHeaderRecyclerView.setItemAnimator(null)

        tableView.unSelectedColor = requireContext().getMaterialColor(
            com.google.android.material.R.attr.colorSecondaryContainer,
            "TableView"
        )

        return fragmentView
    }

    private fun setupFilter(tableFilter: Filter) {
        filterLayout.setEndIconOnClickListener { _: View? ->
            filter.setText("")
            tableFilter.set("")
        }
        filter.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Intentionally empty
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Intentionally empty
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                tableFilter.set(s.toString())
            }
        })
    }
}