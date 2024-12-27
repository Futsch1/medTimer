package com.futsch1.medtimer.helpers;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public abstract class IdlingListAdapter<T, VH extends RecyclerView.ViewHolder>
        extends ListAdapter<T, VH> {
    private final InitIdlingResource idlingResource;

    protected IdlingListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback, String name) {
        super(diffCallback);
        idlingResource = new InitIdlingResource(name);
    }

    @Override
    public void onCurrentListChanged(@NonNull List<T> previousList, @NonNull List<T> currentList) {
        idlingResource.setInitialized();
    }
}