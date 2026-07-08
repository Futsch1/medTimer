package com.futsch1.medtimer.wear

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.wear.tiles.TileService
import kotlinx.coroutines.launch

/**
 * Invisible activity launched by [PillTileService]'s tap action: marks the next due dose as
 * taken and closes immediately, no UI of its own (see the translucent theme in the manifest).
 */
class MarkTakenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            WatchDataStore.refresh(applicationContext)
            val next = WatchDataStore.items.value
                .filter { it.status == "RAISED" }
                .minByOrNull { it.remindedEpochSecond }

            val messageResId = if (next == null) {
                R.string.empty_today
            } else if (WatchActionSender.send(applicationContext, WearProtocol.ACTION_TAKEN, next.reminderEventId)) {
                // A variable-amount dose still needs the amount typed in on the phone - the same
                // fallback already used for a notification's "Taken" action, see
                // ReminderProcessorBroadcastReceiver.requestReminderAction.
                if (next.variableAmount) R.string.action_sent_variable_amount else R.string.action_sent_taken
            } else {
                R.string.phone_not_connected
            }
            Toast.makeText(applicationContext, messageResId, Toast.LENGTH_SHORT).show()
            TileService.getUpdater(applicationContext).requestUpdate(PillTileService::class.java)
            finish()
        }
    }
}
