package com.futsch1.medtimer

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_TIME_STRING
import com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.reminders.notificationFactory.NotificationIntentBuilder
import com.futsch1.medtimer.reminders.notificationFactory.NotificationStringBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderAlarmActivity(private val ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO) : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.alarm)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)

        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setupReminderContent()
    }

    private fun setupReminderContent() {
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_EVENT_ID, -1)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val remindTime = intent.getStringExtra(EXTRA_NOTIFICATION_TIME_STRING)

        val medicineRepository = MedicineRepository(application)

        lifecycleScope.launch {
            withContext(ioCoroutineDispatcher) {
                val reminderEvent = medicineRepository.getReminderEvent(reminderId)

                if (reminderEvent != null && remindTime != null) {
                    Log.d("ReminderAlarmActivity", "Starting activity for reminder ID $reminderId and notification ID $notificationId")
                    val reminder = medicineRepository.getReminder(reminderEvent.reminderId)
                    val medicine = medicineRepository.getMedicine(reminder.medicineRelId)

                    val notificationStrings = NotificationStringBuilder(this@ReminderAlarmActivity, medicine, reminder, remindTime, false)
                    val intents = NotificationIntentBuilder(this@ReminderAlarmActivity, notificationId, reminderEvent, reminder)

                    setupTexts(notificationStrings, medicine)
                    setupButtons(intents)
                }
            }
        }
    }

    private fun setupTexts(notificationStrings: NotificationStringBuilder, medicine: FullMedicine) {
        val notificationTitle = findViewById<TextView>(R.id.notificationTitle)
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

    private fun setupButtons(intents: NotificationIntentBuilder) {
        val takenButton = findViewById<TextView>(R.id.takenButton)
        takenButton.setOnClickListener {
            intents.pendingTaken.send()
            finish()
        }

        val skippedButton = findViewById<TextView>(R.id.skippedButton)
        skippedButton.setOnClickListener {
            intents.pendingSkipped.send()
            finish()
        }

        val snoozeButton = findViewById<TextView>(R.id.snoozeButton)
        snoozeButton.setOnClickListener {
            intents.pendingSnooze.send()
            finish()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

}
