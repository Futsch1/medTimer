package com.futsch1.medtimer.helpers;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public abstract class IdlingListAdapter<T, VH extends RecyclerView.ViewHolder>
        extends ListAdapter<T, VH> {
    private static final InitIdlingResource idlingResource = new InitIdlingResource("ListAdapter");

    protected IdlingListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        super(diffCallback);
        idlingResource.resetInitialized();
    }

    @Override
    public void onCurrentListChanged(@NonNull List<T> previousList, @NonNull List<T> currentList) {
        idlingResource.setInitialized();
    }
}