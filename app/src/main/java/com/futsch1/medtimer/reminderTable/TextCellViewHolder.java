package com.futsch1.medtimer.reminderTable;

import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

public class TextCellViewHolder extends AbstractViewHolder {

    public TextView textView;

    public TextCellViewHolder(TextView textView) {
        super(textView);
        this.textView = textView;
    }
}
