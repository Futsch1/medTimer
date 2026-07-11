package com.futsch1.medtimer.wear

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.futsch1.medtimer.wear.theme.MedTimerWearTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { WatchDataStore.refresh(applicationContext) }

        setContent {
            MedTimerWearTheme {
                var selected by remember { mutableStateOf<WatchReminderItem?>(null) }
                val items by WatchDataStore.items.collectAsStateWithLifecycle()

                Box(modifier = Modifier.fillMaxSize()) {
                    val current = selected
                    if (current == null) {
                        ReminderListScreen(items = items, onSelect = { selected = it })
                    } else {
                        ReminderActionScreen(
                            item = current,
                            onAction = { action ->
                                lifecycleScope.launch {
                                    val sent = WatchActionSender.send(applicationContext, action, current.reminderEventId)
                                    val messageResId = if (!sent) {
                                        R.string.phone_not_connected
                                    } else {
                                        when (action) {
                                            WearProtocol.ACTION_TAKEN -> R.string.action_sent_taken
                                            WearProtocol.ACTION_SKIPPED -> R.string.action_sent_skipped
                                            WearProtocol.ACTION_SNOOZE -> R.string.action_sent_snooze
                                            else -> R.string.action_sent_snooze_home
                                        }
                                    }
                                    Toast.makeText(applicationContext, messageResId, Toast.LENGTH_SHORT).show()
                                }
                                selected = null
                            },
                            onCancel = { selected = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderListScreen(items: List<WatchReminderItem>, onSelect: (WatchReminderItem) -> Unit) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.empty_today))
        }
        return
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 24.dp)
    ) {
        items(items, key = { it.reminderEventId }) { item ->
            Button(
                onClick = { onSelect(item) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(text = item.medicineName)
                    Text(text = stringResource(R.string.status_pending, item.amount, formatTime(item.remindedEpochSecond)))
                }
            }
        }
    }
}

@Composable
private fun ReminderActionScreen(
    item: WatchReminderItem,
    onAction: (String) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = item.medicineName, style = MaterialTheme.typography.titleMedium)
        Button(onClick = { onAction(WearProtocol.ACTION_TAKEN) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_taken))
        }
        Button(onClick = { onAction(WearProtocol.ACTION_SKIPPED) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_skipped))
        }
        Button(onClick = { onAction(WearProtocol.ACTION_SNOOZE) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_snooze))
        }
        Button(onClick = { onAction(WearProtocol.ACTION_SNOOZE_HOME) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_snooze_home))
        }
        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

private fun formatTime(epochSecond: Long): String {
    val time = Instant.ofEpochSecond(epochSecond).atZone(ZoneId.systemDefault()).toLocalTime()
    return time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}
