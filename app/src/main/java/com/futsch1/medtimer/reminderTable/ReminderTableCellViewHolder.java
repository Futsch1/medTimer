package com.futsch1.medtimer.reminderTable;

import android.view.View;
import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import com.futsch1.medtimer.R;

public class ReminderTableCellViewHolder extends AbstractViewHolder {

    public TextView textView;

    public ReminderTableCellViewHolder(View view) {
        super(view);
        this.textView = view.findViewById(R.id.tableCellTextView);
    }
}
