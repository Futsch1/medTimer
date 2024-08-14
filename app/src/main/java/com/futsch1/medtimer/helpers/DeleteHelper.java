package com.futsch1.medtimer.helpers;

import android.app.AlertDialog;
import android.content.Context;

import com.futsch1.medtimer.R;

public class DeleteHelper {
    private final Context context;

    public DeleteHelper(Context context) {
        this.context = context;
    }

    public void deleteItem(int messageStringId, ButtonCallback yesClicked, ButtonCallback noClicked) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.confirm);
        builder.setMessage(messageStringId);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> yesClicked.onButtonClick());
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> noClicked.onButtonClick());
        builder.show();
    }

    public interface ButtonCallback {
        void onButtonClick();
    }
}
