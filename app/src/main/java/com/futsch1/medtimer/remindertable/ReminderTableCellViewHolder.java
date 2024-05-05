package com.futsch1.medtimer.remindertable;

import android.view.View;
import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import com.futsch1.medtimer.R;
import com.google.android.material.color.MaterialColors;

public class ReminderTableCellViewHolder extends AbstractViewHolder {

    private final TextView textView;

    public ReminderTableCellViewHolder(View view) {
        super(view);
        this.textView = view.findViewById(R.id.tableCellTextView);
        this.textView.setTextColor(MaterialColors.getColor(view.getContext(), com.google.android.material.R.attr.colorOnSecondaryContainer, "TableView"));
    }

    public TextView getTextView() {
        return textView;
    }
}
