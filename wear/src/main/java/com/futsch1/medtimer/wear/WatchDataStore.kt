package com.futsch1.medtimer.wear

import android.content.Context
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

/**
 * Holds the last snapshot of today's reminders received from the phone. Updated either by
 * [WatchDataListenerService] (while the watch app isn't in the foreground) or by [refresh] (a
 * one-shot fetch used on app launch, since the listener service only fires on future changes).
 */
object WatchDataStore {
    private val gson = Gson()
    private val itemListType = TypeToken.getParameterized(List::class.java, WatchReminderItem::class.java).type

    private val _items = MutableStateFlow<List<WatchReminderItem>>(emptyList())
    val items: StateFlow<List<WatchReminderItem>> = _items

    fun update(json: String) {
        _items.value = runCatching { gson.fromJson<List<WatchReminderItem>>(json, itemListType) }
            .getOrNull() ?: emptyList()
    }

    suspend fun refresh(context: Context) {
        val dataItems = Wearable.getDataClient(context).dataItems.await()
        try {
            for (dataItem in dataItems) {
                if (dataItem.uri.path == WearProtocol.TODAY_DATA_PATH) {
                    val map = DataMapItem.fromDataItem(dataItem).dataMap
                    map.getString(WearProtocol.TODAY_DATA_KEY)?.let { update(it) }
                }
            }
        } finally {
            dataItems.release()
        }
    }
}
