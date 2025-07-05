package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RemoteViews
import com.futsch1.medtimer.R


class BigReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    reminderNotificationData: ReminderNotificationData
) : ReminderNotificationFactory(
    context,
    notificationId,
    reminderNotificationData
) {
    val views: RemoteViews = RemoteViews(context.packageName, R.layout.notification)
    fun displayBigSurface (context: Context, windowManager: WindowManager): View {
        var  inflater:LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)  as LayoutInflater;
        var dd = FrameLayout(context);
        val view =  inflater.inflate(R.layout.notification, dd);

        val  mp: MediaPlayer = MediaPlayer.create(context, R.raw.sound);

        mp.setOnCompletionListener { mp -> mp.release() }
        mp.start();

        view.findViewById<Button>(R.id.takenButton)
            .setOnClickListener {
                pendingTaken?.send();
                windowManager.removeView(view)
            }
        view.findViewById<Button>(R.id.skippedButton)
            .setOnClickListener {
                pendingSkipped?.send();
                windowManager.removeView(view)
            }
        view.findViewById<Button>(R.id.snoozeButton)
            .setOnClickListener {
                pendingSnooze?.send();
                windowManager.removeView(view)
            }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0,
            PixelFormat.OPAQUE
        )
        params.gravity = Gravity.CENTER or Gravity.START
        params.x = 0
        params.y = 0

        windowManager.addView(view, params)
        return view;
    }

    override fun build() {
        views.setTextViewText(
            R.id.notificationTitle,
            getNotificationString()
        )

        views.setOnClickPendingIntent(R.id.takenButton, pendingTaken)
        views.setOnClickPendingIntent(R.id.skippedButton, pendingSkipped)
        views.setOnClickPendingIntent(R.id.snoozeButton, pendingSnooze)
        if (hasSameTimeReminders) {
            views.setViewVisibility(R.id.allTakenButton, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.allTakenButton, pendingAllTaken)
            views.setTextViewText(R.id.allTakenButton, context.getString(R.string.all_taken, remindTime))
        }
        views.setTextViewCompoundDrawablesRelative(
            R.id.notificationTitle,
            if (medicine.medicine.isOutOfStock) R.drawable.exclamation_triangle_fill else 0,
            0,
            0,
            0
        )

        builder.setCustomBigContentView(views)
        builder.setContentText(baseString)

        buildActions()
    }

    override fun showOutOfStockIcon(): Boolean {
        return false
    }

}