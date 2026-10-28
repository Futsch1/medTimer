package com.futsch1.medtimer.feature.ui.medicine

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.getHelp
import com.futsch1.medtimer.core.ui.getIcon
import com.futsch1.medtimer.core.ui.getTitle
import java.time.LocalTime
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

    if (showTypeHelp) {
        AlertDialog(
            onDismissRequest = { showTypeHelp = false },
            icon = { Icon(painterResource(reminder.type.getIcon()), contentDescription = null) },
            title = { Text(stringResource(reminder.type.getTitle())) },
            text = { Text(stringResource(reminder.type.getHelp())) },
            confirmButton = { TextButton(onClick = { showTypeHelp = false }) { Text(stringResource(R.string.ok)) } },
        )
    }

    ElevatedCard(Modifier
        .testTag(EditMedicineTestTags.REMINDER_CARD)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 5.dp)) {
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
                    EditValueButton(
                        label = stringResource(reminder.timeLabel),
                        value = timeText,
                        onClick = {
                            showMaterialTimePicker(
                                context = context,
                                time = LocalTime.of(minutes / 60, minutes % 60),
                                titleText = reminder.timeHint,
                                force24Hour = reminder.timeHint != null,
                            ) { selectedTime ->
                                val selectedMinutes = selectedTime.hour * 60 + selectedTime.minute
                                if (selectedMinutes != minutes) onEditTime(reminder.id, selectedMinutes)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                    )
                }
                if (reminder.type != ReminderType.OUT_OF_STOCK && reminder.type != ReminderType.EXPIRATION_DATE) {
                    EditTextButton(
                        label = stringResource(R.string.dosage),
                        value = reminder.amount,
                        onConfirm = { onEditAmount(reminder.id, it) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                    )
                }
            }
            Button(onClick = { onReminderEdit(reminder.id, reminder.type) }, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.gear), contentDescription = null)
                Text(stringResource(R.string.advanced_settings), modifier = Modifier.padding(start = 8.dp))
            }
            Text(reminder.summary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
