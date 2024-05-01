package com.futsch1.medtimer.remindertable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evrencoskun.tableview.filter.IFilterableModel;
import com.evrencoskun.tableview.sort.ISortableModel;

public class ReminderTableCellModel implements ISortableModel, IFilterableModel {
    private final Object content;
    private final int id;
    private final String representation;
    private final String viewTag;

    public ReminderTableCellModel(Object content, String representation, int id, String viewTag) {
        this.content = content;
        this.representation = representation;
        this.id = id;
        this.viewTag = viewTag;
    }

    @NonNull
    @Override
    public String getId() {
        return Integer.toString(id);
    }

    @Nullable
    @Override
    public Object getContent() {
        return content;
    }

    @NonNull
    @Override
    @SuppressWarnings("java:S4144")
    public String getFilterableKeyword() {
        return getRepresentation();
    }

    public String getRepresentation() {
        return representation;
    }

    public String getViewTag() {
        return viewTag;
    }
}
