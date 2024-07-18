package com.futsch1.medtimer.remindertable;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evrencoskun.tableview.ITableView;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractSorterViewHolder;
import com.evrencoskun.tableview.sort.SortState;
import com.futsch1.medtimer.R;

/**
 * Created by evrencoskun on 23/10/2017.
 */

public class ReminderTableColumnHeaderViewHolder extends AbstractSorterViewHolder {

    @NonNull
    private final LinearLayout columnHeaderContainer;
    @NonNull
    private final TextView columnHeaderTextview;
    @NonNull
    private final ImageButton columnHeaderSortButton;

    public ReminderTableColumnHeaderViewHolder(@NonNull View itemView, @NonNull ITableView tableView) {
        super(itemView);
        columnHeaderTextview = itemView.findViewById(R.id.tableColumnHeaderTextView);
        columnHeaderContainer = itemView.findViewById(R.id.tableColumnHeaderContainer);
        columnHeaderSortButton = itemView.findViewById(R.id.tableColumnHeaderSortButton);

        // Set click listener to the sort button
        // Default one
        View.OnClickListener mSortButtonClickListener = view -> {
            if (getSortState() == SortState.ASCENDING) {
                tableView.sortColumn(getBindingAdapterPosition(), SortState.UNSORTED);
            } else if (getSortState() == SortState.DESCENDING) {
                tableView.sortColumn(getBindingAdapterPosition(), SortState.ASCENDING);
            } else if (getSortState() == SortState.UNSORTED) {
                // Default one
                tableView.sortColumn(getBindingAdapterPosition(), SortState.DESCENDING);
            }
        };
        columnHeaderSortButton.setOnClickListener(mSortButtonClickListener);
        itemView.setOnClickListener(mSortButtonClickListener);
    }

    /**
     * This method is calling from onBindColumnHeaderHolder on TableViewAdapter
     */
    public void setColumnHeader(@Nullable String columnHeader, boolean firstColumn) {
        columnHeaderTextview.setText(columnHeader);

        if (firstColumn) {
            onSortingStatusChanged(SortState.DESCENDING);
        }

        columnHeaderContainer.getLayoutParams().width = WRAP_CONTENT;
        columnHeaderTextview.requestLayout();
    }

    @Override
    public void onSortingStatusChanged(@NonNull SortState sortState) {
        super.onSortingStatusChanged(sortState);

        columnHeaderContainer.getLayoutParams().width = WRAP_CONTENT;

        controlSortState(sortState);

        columnHeaderTextview.requestLayout();
        columnHeaderSortButton.requestLayout();
        columnHeaderContainer.requestLayout();
        itemView.requestLayout();
    }

    private void controlSortState(@NonNull SortState sortState) {
        if (sortState == SortState.ASCENDING) {
            columnHeaderSortButton.setVisibility(View.VISIBLE);
            columnHeaderSortButton.setImageResource(R.drawable.sort_up);
        } else if (sortState == SortState.DESCENDING) {
            columnHeaderSortButton.setVisibility(View.VISIBLE);
            columnHeaderSortButton.setImageResource(R.drawable.sort_down);
        } else {
            columnHeaderSortButton.setVisibility(View.GONE);
        }
    }
}