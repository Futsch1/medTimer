package com.futsch1.medtimer.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

@SuppressWarnings("java:S119") // Reproducing the naming of the ListAdapter class
public abstract class IdlingListAdapter<T, VH extends RecyclerView.ViewHolder>
        extends ListAdapter<T, VH> {

    private final InitIdlingResource idlingResource;

    protected IdlingListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        this(diffCallback, "IdlingListAdapter");
    }

    protected IdlingListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback, String idlingResourceName) {
        super(diffCallback);
        idlingResource = IdlingResourcesPool.getInstance().getResource(idlingResourceName);
        idlingResource.resetInitialized();
    }

    @Override
    public void submitList(@Nullable List<T> list) {
        super.submitList(list);
        idlingResource.setInitialized();
    }

    @Override
    public void submitList(@Nullable List<T> list, Runnable commitCallback) {
        super.submitList(list, commitCallback);
        idlingResource.setInitialized();
    }

    public void resetInitialized() {
        idlingResource.resetInitialized();
    }
}