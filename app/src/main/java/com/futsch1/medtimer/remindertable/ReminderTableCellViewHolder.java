package com.futsch1.medtimer.remindertable;

import android.graphics.Paint;
import android.view.View;
import android.widget.TextView;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import com.futsch1.medtimer.R;
import com.google.android.material.color.MaterialColors;

public class ReminderTableCellViewHolder extends AbstractViewHolder {

    private final TextView textView;

    public ReminderTableCellViewHolder(View view) {
        super(view);
        textView = view.findViewById(R.id.tableCellTextView);
        textView.setTextColor(MaterialColors.getColor(view.getContext(), com.google.android.material.R.attr.colorOnSecondaryContainer, "TableView"));
        textView.setClickable(false);
        textView.setPaintFlags(textView.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
    }

    public TextView getTextView() {
        return textView;
    }

    public void setupEditButton(OnEditClickListener clickListener) {
        textView.setOnClickListener(v -> clickListener.onEditClick());
        textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    public interface OnEditClickListener {
        void onEditClick();
    }
}
