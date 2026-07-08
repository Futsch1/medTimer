package com.futsch1.medtimer.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/** Receives the "today's reminders" snapshot whenever the phone pushes an update. */
class WatchDataListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            for (event in dataEvents) {
                if (event.type != DataEvent.TYPE_DELETED && event.dataItem.uri.path == WearProtocol.TODAY_DATA_PATH) {
                    val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                    map.getString(WearProtocol.TODAY_DATA_KEY)?.let { WatchDataStore.update(it) }
                }
            }
        } finally {
            dataEvents.release()
        }
    }
}
