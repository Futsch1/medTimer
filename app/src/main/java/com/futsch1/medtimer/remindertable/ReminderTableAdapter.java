package com.futsch1.medtimer.remindertable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.evrencoskun.tableview.TableView;
import com.evrencoskun.tableview.adapter.AbstractTableAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.helpers.TimeHelper;

import java.util.ArrayList;
import java.util.List;

public class ReminderTableAdapter extends AbstractTableAdapter<String, ReminderTableCellModel, ReminderTableCellModel> {
    private final TableView tableView;

    public ReminderTableAdapter(TableView tableView) {
        this.tableView = tableView;
    }

    @NonNull
    @Override
    public AbstractViewHolder onCreateCellViewHolder(@NonNull ViewGroup parent, int viewType) {
        return getTextCellViewHolder(parent);
    }

    @NonNull
    private static ReminderTableCellViewHolder getTextCellViewHolder(ViewGroup parent) {
        View layout = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.reminder_table_cell, parent, false);
        return new ReminderTableCellViewHolder(layout);
    }

    @Override
    public void onBindCellViewHolder(@NonNull AbstractViewHolder holder, ReminderTableCellModel cellItemModel, int
            columnPosition, int rowPosition) {
        ReminderTableCellViewHolder viewHolder = (ReminderTableCellViewHolder) holder;
        if (cellItemModel != null) {
            String modelContent = cellItemModel.getRepresentation();
            viewHolder.getTextView().setText(modelContent);
            viewHolder.getTextView().setTag(cellItemModel.getViewTag());
        }

        viewHolder.getTextView().requestLayout();
    }

    @NonNull
    @Override
    public AbstractViewHolder onCreateColumnHeaderViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.reminder_table_column_header, parent, false);
        return new ReminderTableColumnHeaderViewHolder(layout, tableView);
    }

    @Override
    public void onBindColumnHeaderViewHolder(@NonNull AbstractViewHolder holder, String columnHeaderItemModel, int
            position) {
        ReminderTableColumnHeaderViewHolder columnHeaderViewHolder = (ReminderTableColumnHeaderViewHolder) holder;
        columnHeaderViewHolder.setColumnHeader(columnHeaderItemModel, position == 0);
    }

    @NonNull
    @Override
    public AbstractViewHolder onCreateRowHeaderViewHolder(@NonNull ViewGroup parent, int viewType) {
        return getTextCellViewHolder(parent);
    }

    @Override
    public void onBindRowHeaderViewHolder(@NonNull AbstractViewHolder abstractViewHolder, ReminderTableCellModel s, int i) {
        onBindCellViewHolder(abstractViewHolder, s, i, i);
    }

    @NonNull
    @Override
    public View onCreateCornerView(@NonNull ViewGroup viewGroup) {
        return new View(viewGroup.getContext());
    }

    public void submitList(List<ReminderEvent> reminderEvents) {
        List<List<ReminderTableCellModel>> cells = new ArrayList<>();
        List<ReminderTableCellModel> rows = new ArrayList<>();

        for (ReminderEvent reminderEvent : reminderEvents) {
            List<ReminderTableCellModel> cell = new ArrayList<>();
            cell.add(new ReminderTableCellModel(reminderEvent.remindedTimestamp, TimeHelper.toLocalizedDatetimeString(tableView.getContext(), reminderEvent.remindedTimestamp), reminderEvent.reminderEventId, null));
            cell.add(new ReminderTableCellModel(reminderEvent.medicineName, reminderEvent.medicineName, reminderEvent.reminderEventId, "medicineName"));
            cell.add(new ReminderTableCellModel(reminderEvent.amount, reminderEvent.amount, reminderEvent.reminderEventId, null));
            cell.add(new ReminderTableCellModel(reminderEvent.status,
                    reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN ?
                            TimeHelper.toLocalizedDatetimeString(tableView.getContext(), reminderEvent.processedTimestamp) : " ",
                    reminderEvent.reminderEventId, null));
            cells.add(cell);
            rows.add(new ReminderTableCellModel(reminderEvent.reminderEventId, Integer.toString(reminderEvent.reminderEventId), reminderEvent.reminderEventId, null));
        }

        setCellItems(cells);
        // This is not used in the table, but required for the filter to work
        setRowHeaderItems(rows);
    }
}
