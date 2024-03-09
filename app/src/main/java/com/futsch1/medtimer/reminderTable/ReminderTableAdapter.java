package com.futsch1.medtimer.reminderTable;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evrencoskun.tableview.adapter.AbstractTableAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.util.ArrayList;
import java.util.List;

public class ReminderTableAdapter extends AbstractTableAdapter<String, String, String> {

    @NonNull
    @Override
    public AbstractViewHolder onCreateCellViewHolder(@NonNull ViewGroup parent, int viewType) {
        return getTextCellViewHolder(parent);
    }

    @Override
    public void onBindCellViewHolder(@NonNull AbstractViewHolder holder, String cellItemModel, int
            columnPosition, int rowPosition) {
        TextCellViewHolder viewHolder = (TextCellViewHolder) holder;
        viewHolder.textView.setText(cellItemModel);

        viewHolder.textView.requestLayout();
    }

    @NonNull
    @Override
    public AbstractViewHolder onCreateColumnHeaderViewHolder(@NonNull ViewGroup parent, int viewType) {
        return getTextCellViewHolder(parent);
    }

    @Override
    public void onBindColumnHeaderViewHolder(@NonNull AbstractViewHolder holder, String columnHeaderItemModel, int
            position) {
        TextCellViewHolder columnHeaderViewHolder = (TextCellViewHolder) holder;
        columnHeaderViewHolder.textView.setText(columnHeaderItemModel);

        columnHeaderViewHolder.textView.requestLayout();
    }

    @NonNull
    @Override
    public AbstractViewHolder onCreateRowHeaderViewHolder(@NonNull ViewGroup parent, int viewType) {
        return getTextCellViewHolder(parent);
    }

    @Override
    public void onBindRowHeaderViewHolder(@NonNull AbstractViewHolder abstractViewHolder, @Nullable String s, int i) {
        TextCellViewHolder rowHeaderViewHolder = (TextCellViewHolder) abstractViewHolder;
        rowHeaderViewHolder.textView.setText(s);

        rowHeaderViewHolder.textView.requestLayout();
    }

    @NonNull
    @Override
    public View onCreateCornerView(@NonNull ViewGroup viewGroup) {
        return new View(viewGroup.getContext());
    }

    @NonNull
    private static TextCellViewHolder getTextCellViewHolder(ViewGroup parent) {
        TextView layout = new TextView(parent.getContext());
        return new TextCellViewHolder(layout);
    }

    public void submitList(List<ReminderEvent> reminderEvents) {
        List<List<String>> cells = new ArrayList<>();

        for (ReminderEvent reminderEvent : reminderEvents) {
            List<String> cell = new ArrayList<>();
            cell.add(reminderEvent.medicineName);
            cell.add(reminderEvent.amount);
            cell.add(reminderEvent.status.toString());
            cells.add(cell);
        }

        setCellItems(cells);
    }
}
