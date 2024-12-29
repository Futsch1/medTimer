package com.futsch1.medtimer.helpers;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

@SuppressWarnings("java:S119") // Reproducing the naming of the ListAdapter class
public abstract class IdlingListAdapter<T, VH extends RecyclerView.ViewHolder>
        extends ListAdapter<T, VH> {
    private static final InitIdlingResource idlingResource = new InitIdlingResource("ListAdapter");
    private static final InitIdlingResource idlingResource2 = new InitIdlingResource("ListAdapter");

    private InitIdlingResource usedResource = idlingResource;

    protected IdlingListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        super(diffCallback);
        usedResource.resetInitialized();
    }

    protected IdlingListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback, boolean useSecondResource) {
        super(diffCallback);
        usedResource = useSecondResource ? idlingResource2 : idlingResource;
        usedResource.resetInitialized();
    }

    @Override
    public void onCurrentListChanged(@NonNull List<T> previousList, @NonNull List<T> currentList) {
        usedResource.setInitialized();
    }

    public void resetInitialized() {
        usedResource.resetInitialized();
    }
}