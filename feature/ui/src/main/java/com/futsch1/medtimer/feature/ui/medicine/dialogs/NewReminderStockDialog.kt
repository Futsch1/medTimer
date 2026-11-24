package com.futsch1.medtimer.feature.ui.medicine.dialogs

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.feature.ui.medicine.NewReminderViewModel

/** Starts a draft for an out-of-stock or expiration date reminder, prefilled from [medicine]'s stock. */
fun NewReminderViewModel.startStockReminder(medicine: Medicine, type: ReminderType) {
    require(type == ReminderType.OUT_OF_STOCK || type == ReminderType.EXPIRATION_DATE)
    startNewReminder(medicineId = medicine.id, amount = medicine.amount)
    updateDraft(
        state.draft.copy(
            selectedType = type,
            threshold = if (medicine.amount > 0.0) medicine.amount.toInt().toString() else "1",
        )
    )
}

/** Creates the reminder drafted with [startStockReminder]. */
@Composable
fun NewReminderStockDialog(
    medicineUnit: String,
    onCreated: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: NewReminderViewModel = hiltViewModel(),
) {
    NewReminderFlowDialog(
        state = viewModel.state,
        medicineUnit = medicineUnit,
        onCreate = {
            if (viewModel.createReminder()) onCreated()
        },
        onDraftChange = viewModel::updateDraft,
        onDismiss = onDismiss,
        onOpenHelp = onDismiss,
    )
}
