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
import com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.reminders.notificationFactory.NotificationIntentBuilder
import com.futsch1.medtimer.reminders.notificationFactory.NotificationStringBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmFragment(
    private val ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : Fragment() {
    var reminderEventId: Int = -1
    var notificationId: Int = -1
    lateinit var remindTime: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = requireArguments()
        reminderEventId = bundle.getInt(EXTRA_REMINDER_EVENT_ID, -1)
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
                val medicineRepository = MedicineRepository(requireActivity().application)

                val reminderEvent = medicineRepository.getReminderEvent(reminderEventId)

                if (reminderEvent != null) {
                    Log.d("AlarmFragment", "Creating fragment for reminder ID $reminderEventId and notification ID $notificationId")
                    val reminder = medicineRepository.getReminder(reminderEvent.reminderId)
                    val medicine = medicineRepository.getMedicine(reminder.medicineRelId)

                    val notificationStrings = NotificationStringBuilder(requireContext(), medicine, reminder, remindTime, false)
                    val intents = NotificationIntentBuilder(requireContext(), notificationId, reminderEvent, reminder)

                    withContext(mainDispatcher) {
                        setupTexts(view, notificationStrings, medicine)
                        setupButtons(view, intents)
                    }
                }
            }
        }


        return view
    }

    private fun setupTexts(view: View, notificationStrings: NotificationStringBuilder, medicine: FullMedicine) {
        val notificationTitle = view.findViewById<TextView>(R.id.notificationTitle)
        notificationTitle.text = notificationStrings.notificationString
        if (medicine.medicine.isOutOfStock) {
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
        if (requireActivity().supportFragmentManager.fragments.isEmpty()) {
            requireActivity().finish()
        }
    }
}