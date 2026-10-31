package com.futsch1.medtimer.feature.ui.medicine.dialogs

import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import com.futsch1.medtimer.feature.ui.medicine.NewReminderViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class NewReminderStockDialog @AssistedInject constructor(
    @Assisted private val activity: FragmentActivity,
    @Assisted private val medicine: Medicine,
    @Assisted private val isOutOfStockReminder: Boolean
) {
    @AssistedFactory
    fun interface Factory {
        fun create(
            activity: FragmentActivity,
            medicine: Medicine,
            isOutOfStockReminder: Boolean
        ): NewReminderStockDialog
    }

    private val host = ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    }
    private val root = activity.window.decorView as ViewGroup
    private val newReminderViewModel = ViewModelProvider(activity)[NewReminderViewModel::class.java]

    init {
        newReminderViewModel.startNewReminder(
            medicineId = medicine.id,
            amount = medicine.amount
        )
        newReminderViewModel.updateDraft(
            newReminderViewModel.state.draft.copy(
                selectedType = if (isOutOfStockReminder) ReminderType.OUT_OF_STOCK else ReminderType.EXPIRATION_DATE,
                threshold = if (medicine.amount > 0.0) medicine.amount.toInt().toString() else "1",
            )
        )
        root.addView(host, ViewGroup.LayoutParams(1, 1))
        host.setContent {
            MedTimerTheme {
                NewReminderFlowDialog(
                    state = newReminderViewModel.state,
                    medicineUnit = MedicineHelper.getUnit(medicine),
                    onCreate = {
                        if (newReminderViewModel.createReminder()) reminderCreated()
                    },
                    onDraftChange = newReminderViewModel::updateDraft,
                    onDismiss = ::dismiss,
                    onOpenHelp = ::dismiss,
                )
            }
        }
    }

    private fun reminderCreated() {
        Toast.makeText(
            activity,
            com.futsch1.medtimer.core.ui.R.string.successfully_created_reminder,
            Toast.LENGTH_LONG,
        ).show()
        dismiss()
    }

    private fun dismiss() {
        root.removeView(host)
    }
}
