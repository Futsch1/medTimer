package com.futsch1.medtimer.feature.ui.medicine

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.getHelp
import com.futsch1.medtimer.core.ui.getIcon
import com.futsch1.medtimer.core.ui.getTitle
import java.util.Calendar

@Composable
fun ReminderCard(
    reminder: ReminderItem,
    onReminderEdit: (id: Int, type: ReminderType) -> Unit,
    onEditAmount: (id: Int, amount: String) -> Unit,
    onEditTime: (id: Int, timeMinutesOfDay: Int) -> Unit
) {
    val context = LocalContext.current
    var showTypeHelp by rememberSaveable(reminder.id) { mutableStateOf(false) }
    var amount by rememberSaveable(reminder.id) { mutableStateOf(reminder.amount) }
    var amountFocused by rememberSaveable(reminder.id) { mutableStateOf(false) }
    LaunchedEffect(reminder.amount, amountFocused) {
        if (!amountFocused) amount = reminder.amount
    }

    if (showTypeHelp) {
        AlertDialog(
            onDismissRequest = { showTypeHelp = false },
            icon = { Icon(painterResource(reminder.type.getIcon()), contentDescription = null) },
            title = { Text(stringResource(reminder.type.getTitle())) },
            text = { Text(stringResource(reminder.type.getHelp())) },
            confirmButton = { TextButton(onClick = { showTypeHelp = false }) { Text(stringResource(R.string.ok)) } },
        )
    }

    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 5.dp)) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showTypeHelp = true }) {
                    Icon(
                        painterResource(reminder.type.getIcon()),
                        contentDescription = stringResource(reminder.type.getTitle()),
                        modifier = Modifier.size(32.dp),
                    )
                }
                if (reminder.timeVisible) {
                    val minutes = reminder.timeMinutesOfDay
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, minutes / 60)
                        set(Calendar.MINUTE, minutes % 60)
                    }
                    val timeText = if (reminder.timeHint != null) TimeHelper.minutesToDurationString(minutes)
                    else DateFormat.getTimeFormat(context).format(calendar.time)
                    val hint = reminder.timeHint?.let { stringResource(it) }
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(context, { _, hour, minute -> onEditTime(reminder.id, hour * 60 + minute) },
                                minutes / 60, minutes % 60, reminder.timeHint != null || DateFormat.is24HourFormat(context)
                            ).apply { if (hint != null) setTitle(hint) }.show()
                        },
                        modifier = Modifier.weight(1f).padding(4.dp),
                    ) {
                        Column {
                            Text(stringResource(reminder.timeLabel))
                            Text(timeText)
                        }
                    }
                }
                if (reminder.type != ReminderType.OUT_OF_STOCK && reminder.type != ReminderType.EXPIRATION_DATE) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it; onEditAmount(reminder.id, it) },
                        label = { Text(stringResource(R.string.dosage)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(4.dp).onFocusChanged { amountFocused = it.isFocused },
                    )
                }
            }
            Button(onClick = { onReminderEdit(reminder.id, reminder.type) }, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.gear), contentDescription = null)
                Text(stringResource(R.string.advanced_settings), modifier = Modifier.padding(start = 8.dp))
            }
            Text(reminder.summary)
        }
    }
}
