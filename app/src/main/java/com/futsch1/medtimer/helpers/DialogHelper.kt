package com.futsch1.medtimer.helpers;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.text.Editable;
import android.util.TypedValue;
import android.widget.LinearLayout;

import com.futsch1.medtimer.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class DialogHelper {
    private DialogHelper() {
        // Intentionally empty
    }

    public static void showTextInputDialog(Context context,
                                           int title,
                                           int hint,
                                           TextSink textSink) {
        showTextInputDialog(context, title, hint, "", textSink, null);
    }

    public static void showTextInputDialog(Context context,
                                           int title,
                                           int hint,
                                           String initialText,
                                           TextSink textSink,
                                           CancelSink cancelSink) {
        TextInputLayout textInputLayout = new TextInputLayout(context);
        TextInputEditText editText = new TextInputEditText(context);
        editText.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        editText.setHint(hint);
        editText.setSingleLine();
        editText.setMinimumHeight(dpToPx(context.getResources(), 48));
        editText.setId(android.R.id.input);
        editText.setText(initialText);
        textInputLayout.addView(editText);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(textInputLayout).setTitle(title);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            Editable e = editText.getText();
            if (e != null) {
                textSink.consumeText(e.toString());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
            if (cancelSink != null) {
                cancelSink.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * @noinspection SameParameterValue
     */
    private static int dpToPx(Resources r, int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    public interface TextSink {
        void consumeText(String text);
    }

    public interface CancelSink {
        void cancel();
    }
}
