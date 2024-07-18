package com.futsch1.medtimer.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.recyclerview.widget.RecyclerView;

import com.futsch1.medtimer.R;

public class DeleteHelper<V extends RecyclerView.ViewHolder> {
    private final Context context;
    private final HandlerThread thread;
    private final RecyclerView.Adapter<V> adapter;

    public DeleteHelper(Context context, HandlerThread thread, RecyclerView.Adapter<V> adapter) {
        this.context = context;
        this.thread = thread;
        this.adapter = adapter;
    }

    public void deleteItem(int adapterPosition, int messageStringId, Runnable r) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.confirm);
        builder.setMessage(messageStringId);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
            final Handler handler = new Handler(thread.getLooper());
            handler.post(r);
        });
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> adapter.notifyItemRangeChanged(adapterPosition, adapterPosition + 1));
        builder.show();
    }
}
