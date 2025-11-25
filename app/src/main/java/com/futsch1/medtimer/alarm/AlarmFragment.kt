package com.futsch1.medtimer.alarm

import android.app.PendingIntent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.reminders.notificationFactory.NotificationIntentBuilder
import com.futsch1.medtimer.reminders.notificationFactory.NotificationStringBuilder
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmFragment(
    private val ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : Fragment() {
    lateinit var reminderNotificationData: ReminderNotificationData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = requireArguments()

        reminderNotificationData = ReminderNotificationData.fromBundle(bundle, requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alarm, container, false)

        lifecycleScope.launch {
            withContext(ioCoroutineDispatcher) {
                Log.d("AlarmFragment", "Creating fragment for raised notification $reminderNotificationData")

                val notificationStrings = NotificationStringBuilder(requireContext(), reminderNotificationData, false)
                val intents =
                    NotificationIntentBuilder(
                        requireContext(), reminderNotificationData
                    )

                withContext(mainDispatcher) {
                    setupTexts(view, notificationStrings, reminderNotificationData.notificationReminderEvents.any { it.medicine.medicine.isOutOfStock })
                    setupButtons(view, intents)
                }
            }
        }


        return view
    }

    private fun setupTexts(view: View, notificationStrings: NotificationStringBuilder, anyOutOfStock: Boolean) {
        val notificationTitle = view.findViewById<TextView>(R.id.notificationTitle)
        notificationTitle.text = notificationStrings.notificationString
        if (anyOutOfStock) {
            notificationTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.exclamation_triangle_fill,
                0,
                0,
                0
            )
        }
    }

    private fun setupButtons(view: View, intents: NotificationIntentBuilder) {
        val takenButton = view.findViewById<TextView>(R.id.takenButton)
        takenButton.setOnClickListener {
            closeWithIntent(intents.pendingTaken)
        }

        val skippedButton = view.findViewById<TextView>(R.id.skippedButton)
        skippedButton.setOnClickListener {
            closeWithIntent(intents.pendingSkipped)
        }

        val snoozeButton = view.findViewById<TextView>(R.id.snoozeButton)
        snoozeButton.setOnClickListener {
            closeWithIntent(intents.pendingSnooze)
        }
    }

    private fun closeWithIntent(pendingIntent: PendingIntent) {
        pendingIntent.send()
        requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("AlarmFragment", "Closing activity")
        requireActivity().finishAndRemoveTask()
    }
}