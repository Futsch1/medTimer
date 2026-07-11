package com.futsch1.medtimer.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

/** Sends a Taken/Skip/Snooze action to the phone over the Data Layer's [MessageClient]. */
object WatchActionSender {
    private val gson = Gson()

    /** Returns true if the message was handed to at least one connected phone node. */
    suspend fun send(context: Context, action: String, reminderEventId: Int): Boolean {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        if (nodes.isEmpty()) return false

        val payload = gson.toJson(WatchAction(action, reminderEventId)).toByteArray(Charsets.UTF_8)
        val messageClient = Wearable.getMessageClient(context)
        for (node in nodes) {
            messageClient.sendMessage(node.id, WearProtocol.ACTION_MESSAGE_PATH, payload).await()
        }
        return true
    }
}
