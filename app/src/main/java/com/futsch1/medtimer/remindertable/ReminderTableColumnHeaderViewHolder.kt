package com.futsch1.medtimer.remindertable

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.evrencoskun.tableview.ITableView
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractSorterViewHolder
import com.evrencoskun.tableview.sort.SortState
import com.futsch1.medtimer.R

/**
 * Created by evrencoskun on 23/10/2017.
 */
class ReminderTableColumnHeaderViewHolder(itemView: View, tableView: ITableView) :
    AbstractSorterViewHolder(itemView) {
    private val columnHeaderContainer: LinearLayout =
        itemView.findViewById(R.id.tableColumnHeaderContainer)
    private val columnHeaderTextview: TextView =
        itemView.findViewById(R.id.tableColumnHeaderTextView)
    private val columnHeaderSortButton: ImageButton =
        itemView.findViewById(R.id.tableColumnHeaderSortButton)

    init {
        // Set click listener to the sort button
        // Default one
        val mSortButtonClickListener = View.OnClickListener {
            when (sortState) {
                SortState.ASCENDING -> {
                    tableView.sortColumn(getBindingAdapterPosition(), SortState.UNSORTED)
                }

                SortState.DESCENDING -> {
                    tableView.sortColumn(getBindingAdapterPosition(), SortState.ASCENDING)
                }

                SortState.UNSORTED -> {
                    // Default one
                    tableView.sortColumn(getBindingAdapterPosition(), SortState.DESCENDING)
                }
            }
        }
        columnHeaderSortButton.setOnClickListener(mSortButtonClickListener)
        itemView.setOnClickListener(mSortButtonClickListener)
    }

    /**
     * This method is calling from onBindColumnHeaderHolder on TableViewAdapter
     */
    fun setColumnHeader(columnHeader: String?, firstColumn: Boolean) {
        columnHeaderTextview.text = columnHeader

        if (firstColumn) {
            onSortingStatusChanged(SortState.DESCENDING)
        }

        columnHeaderContainer.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        columnHeaderTextview.requestLayout()
    }

    override fun onSortingStatusChanged(sortState: SortState) {
        super.onSortingStatusChanged(sortState)

        columnHeaderContainer.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT

        controlSortState(sortState)

        columnHeaderTextview.requestLayout()
        columnHeaderSortButton.requestLayout()
        columnHeaderContainer.requestLayout()
        itemView.requestLayout()
    }

    private fun controlSortState(sortState: SortState) {
        when (sortState) {
            SortState.ASCENDING -> {
                columnHeaderSortButton.setVisibility(View.VISIBLE)
                columnHeaderSortButton.setImageResource(R.drawable.sort_up)
            }

            SortState.DESCENDING -> {
                columnHeaderSortButton.setVisibility(View.VISIBLE)
                columnHeaderSortButton.setImageResource(R.drawable.sort_down)
            }

            else -> {
                columnHeaderSortButton.setVisibility(View.GONE)
            }
        }
    }
}