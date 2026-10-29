package com.futsch1.medtimer.feature.ui.medicine.dialogs

import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderTime
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.medicine.NewReminderData
import com.futsch1.medtimer.feature.ui.medicine.NewReminderExpirationFrequency
import com.futsch1.medtimer.feature.ui.medicine.NewReminderStockFrequency
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import java.time.LocalDate

class NewReminderStockDialog @AssistedInject constructor(
    @Assisted private val activity: FragmentActivity,
    @Assisted private val medicine: Medicine,
    @Assisted private val reminder: Reminder,
    private val reminderRepository: ReminderRepository,
    private val timeFormatter: TimeFormatter,
) {
    @AssistedFactory
    fun interface Factory {
        fun create(
            activity: FragmentActivity,
            medicine: Medicine,
            reminder: Reminder,
        ): NewReminderStockDialog
    }

    private val host = ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    }
    private val root = activity.window.decorView as ViewGroup

    init {
        root.addView(host, ViewGroup.LayoutParams(1, 1))
        host.setContent {
            MedTimerTheme {
                NewReminderFlowDialog(
                    medicineId = medicine.id,
                    medicineAmount = reminder.outOfStockThreshold,
                    medicineUnit = medicine.unit,
                    timeFormatter = timeFormatter,
                    initialType = reminder.reminderType,
                    onCreate = ::createReminder,
                    onDismiss = ::dismiss,
                    onOpenHelp = ::dismiss,
                )
            }
        }
    }

    private fun createReminder(data: NewReminderData) {
        val updatedReminder = reminder.copy(time = ReminderTime(data.timeMinutes)).let { base ->
            when (data.type) {
                ReminderType.OUT_OF_STOCK -> base.copy(
                    outOfStockThreshold = checkNotNull(data.stockThreshold),
                    outOfStockReminderType = when (checkNotNull(data.stockFrequency)) {
                        NewReminderStockFrequency.ONCE -> Reminder.OutOfStockReminderType.ONCE
                        NewReminderStockFrequency.ALWAYS -> Reminder.OutOfStockReminderType.ALWAYS
                        NewReminderStockFrequency.DAILY -> Reminder.OutOfStockReminderType.DAILY
                    },
                )

                ReminderType.EXPIRATION_DATE -> base.copy(
                    expirationReminderType = when (checkNotNull(data.expirationFrequency)) {
                        NewReminderExpirationFrequency.ONCE -> Reminder.ExpirationReminderType.ONCE
                        NewReminderExpirationFrequency.DAILY -> Reminder.ExpirationReminderType.DAILY
                    },
                    periodStart = LocalDate.ofEpochDay(checkNotNull(data.expirationDaysBefore)),
                )

                else -> error("Unsupported stock reminder type: ${data.type}")
            }
        }

        activity.lifecycleScope.launch {
            reminderRepository.create(updatedReminder)
            Toast.makeText(
                activity,
                com.futsch1.medtimer.core.ui.R.string.successfully_created_reminder,
                Toast.LENGTH_LONG,
            ).show()
            dismiss()
        }
    }

    private fun dismiss() {
        root.removeView(host)
    }
}
