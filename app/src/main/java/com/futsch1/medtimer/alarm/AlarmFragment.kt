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
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_TIME_STRING
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.reminders.NotificationTriplet
import com.futsch1.medtimer.reminders.notificationFactory.NotificationIntentBuilder
import com.futsch1.medtimer.reminders.notificationFactory.NotificationStringBuilder
import com.futsch1.medtimer.reminders.notificationFactory.NotificationStringTuple
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmFragment(
    private val ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : Fragment() {
    lateinit var notificationTriplets: List<NotificationTriplet>
    var notificationId: Int = -1
    lateinit var remindTime: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = requireArguments()

        notificationTriplets = NotificationTriplet.fromBundle(bundle, MedicineRepository(requireActivity().application))
        notificationId = bundle.getInt(EXTRA_NOTIFICATION_ID, -1)
        remindTime = bundle.getString(EXTRA_NOTIFICATION_TIME_STRING, "?")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alarm, container, false)

        lifecycleScope.launch {
            withContext(ioCoroutineDispatcher) {
                Log.d("AlarmFragment", "Creating fragment for reminder event IDs $notificationTriplets and notification ID $notificationId")

                val notificationStringTuples = NotificationStringTuple.fromNotificationTriplets(notificationTriplets)

                val notificationStrings = NotificationStringBuilder(requireContext(), notificationStringTuples, remindTime, false)
                val intents =
                    NotificationIntentBuilder(
                        requireContext(), notificationId, NotificationTriplet.getReminderEventIds(notificationTriplets),
                        NotificationTriplet.getReminderIds(notificationTriplets)
                    )

                withContext(mainDispatcher) {
                    setupTexts(view, notificationStrings, notificationTriplets.any { it.medicine!!.medicine.isOutOfStock })
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