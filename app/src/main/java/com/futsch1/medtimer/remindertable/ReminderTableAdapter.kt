package com.futsch1.medtimer.remindertable

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.evrencoskun.tableview.TableView
import com.evrencoskun.tableview.adapter.AbstractTableAdapter
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.overview.EditEventSheetDialogFragment
import com.futsch1.medtimer.remindertable.ReminderTableCellViewHolder.OnEditClickListener

class ReminderTableAdapter(
    private val tableView: TableView,
    private val fragmentManager: FragmentManager,
    private val timeFormatter: TimeFormatter
) : AbstractTableAdapter<String?, ReminderTableCellModel?, ReminderTableCellModel?>() {
    override fun onCreateCellViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        return getTextCellViewHolder(parent)
    }

    override fun onBindCellViewHolder(
        holder: AbstractViewHolder,
        cellItemModel: ReminderTableCellModel?,
        columnPosition: Int,
        rowPosition: Int
    ) {
        if (cellItemModel == null) {
            return
        }

        val modelContent = cellItemModel.representation

        val viewHolder = holder as ReminderTableCellViewHolder
        viewHolder.textView.text = modelContent
        viewHolder.textView.tag = cellItemModel.viewTag
        viewHolder.setupEditButton(if (columnPosition == 1) object : OnEditClickListener {
            override fun onEditClick() {
                EditEventSheetDialogFragment.newInstance(cellItemModel.idAsInt)
                    .show(fragmentManager, "EditEventDialog")
            }
        } else null)
    }

    override fun onCreateColumnHeaderViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AbstractViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.reminder_table_column_header, parent, false)
        return ReminderTableColumnHeaderViewHolder(layout, tableView)
    }

    override fun onBindColumnHeaderViewHolder(
        holder: AbstractViewHolder,
        columnHeaderItemModel: String?,
        position: Int
    ) {
        val columnHeaderViewHolder = holder as ReminderTableColumnHeaderViewHolder
        columnHeaderViewHolder.setColumnHeader(columnHeaderItemModel, position == 0)
    }

    override fun onCreateRowHeaderViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        return getTextCellViewHolder(parent)
    }

    override fun onBindRowHeaderViewHolder(
        abstractViewHolder: AbstractViewHolder,
        s: ReminderTableCellModel?,
        i: Int
    ) {
        onBindCellViewHolder(abstractViewHolder, s, i, i)
    }

    override fun onCreateCornerView(viewGroup: ViewGroup): View {
        return View(viewGroup.context)
    }

    fun submitList(reminderEvents: List<ReminderEvent>) {
        val cells = mutableListOf<List<ReminderTableCellModel?>>()
        val rows = mutableListOf<ReminderTableCellModel?>()
        val formatter = timeFormatter

        for (reminderEvent in reminderEvents) {
            val cell = listOf(
                ReminderTableCellModel(
                    reminderEvent.processedTimestamp,
                    getStatusString(reminderEvent, formatter),
                    reminderEvent.reminderEventId, "taken"
                ),
                ReminderTableCellModel(
                    reminderEvent.medicineName,
                    reminderEvent.medicineName,
                    reminderEvent.reminderEventId,
                    "medicineName"
                ),
                ReminderTableCellModel(
                    reminderEvent.amount,
                    reminderEvent.amount,
                    reminderEvent.reminderEventId,
                    null
                ),
                ReminderTableCellModel(
                    reminderEvent.remindedTimestamp,
                    formatter.toDateTimeString(reminderEvent.remindedTimestamp),
                    reminderEvent.reminderEventId,
                    "time"
                )
            )

            cells.add(cell)
            rows.add(
                ReminderTableCellModel(
                    reminderEvent.reminderEventId,
                    reminderEvent.reminderEventId.toString(),
                    reminderEvent.reminderEventId,
                    null
                )
            )
        }

        setCellItems(cells)
        // This is not used in the table, but required for the filter to work
        setRowHeaderItems(rows)
    }

    private fun getStatusString(
        reminderEvent: ReminderEvent,
        formatter: TimeFormatter
    ): String {
        return when (reminderEvent.status) {
            ReminderEvent.ReminderStatus.TAKEN -> formatter.toDateTimeString(reminderEvent.processedTimestamp)
            ReminderEvent.ReminderStatus.RAISED -> " "
            else -> "-"
        }
    }

    companion object {
        private fun getTextCellViewHolder(parent: ViewGroup): ReminderTableCellViewHolder {
            val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.reminder_table_cell, parent, false)
            return ReminderTableCellViewHolder(layout)
        }
    }
}
