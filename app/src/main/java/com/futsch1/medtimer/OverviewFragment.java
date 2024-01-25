package com.futsch1.medtimer;

import static com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OverviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OverviewFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);

        Intent notifyTaken = new Intent(getContext(), TakenService.class);
        notifyTaken.putExtra(EXTRA_NOTIFICATION_ID, 12);
        PendingIntent pendingTaken = PendingIntent.getService(getContext(), 0, notifyTaken, PendingIntent.FLAG_IMMUTABLE);
        Intent notifyDismissed = new Intent(getContext(), TakenService.class);
        PendingIntent pendingDismissed = PendingIntent.getService(getContext(), 0, notifyDismissed, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.requireContext(), "com.medTimer.NOTIFICATIONS")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Test")
                .setContentText("Test2")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingTaken)
                .setDeleteIntent(pendingDismissed)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_launcher_foreground, getString(R.string.notification_taken), pendingTaken);

        notificationManager.notify(12, builder.build());

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_overview, container, false);
    }
}