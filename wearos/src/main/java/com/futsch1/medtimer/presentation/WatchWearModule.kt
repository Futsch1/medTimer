package com.futsch1.medtimer.presentation

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import com.futsch1.medtimer.shared.wear.NotifyTakenData
import com.futsch1.medtimer.shared.wear.NotifyTakenDataSerializer
import com.futsch1.medtimer.shared.wear.TakenData
import com.futsch1.medtimer.shared.wear.TakenDataSerializer
import com.futsch1.medtimer.shared.wear.WearData
import com.futsch1.medtimer.shared.wear.WearDataSerializer
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoDataStore
import com.google.android.horologist.data.ProtoDataStoreHelper.protoFlow
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.apphelper.AppHelperNodeStatus
import com.google.android.horologist.datalayer.watch.WearDataLayerAppHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Suppress("MISSING_DEPENDENCY_SUPERCLASS_WARNING")
@OptIn(ExperimentalHorologistApi::class)
class WatchWearModule(val context: Context) {
    private lateinit var notifyTakenDataStore: DataStore<NotifyTakenData>
    private lateinit var wearDataLayerRegistry: WearDataLayerRegistry
    private lateinit var appHelper: WearDataLayerAppHelper
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    fun wearData(): Flow<WearData> =
        wearDataLayerRegistry.protoFlow(TargetNodeId.PairedPhone)

    fun takenData(): Flow<TakenData> =
        wearDataLayerRegistry.protoFlow(TargetNodeId.PairedPhone)

    init {
        // Use GlobalScope if the data layer should persist for the entire application lifecycle
        // Or, provide a more specific scope if its lifecycle is tied to a shorter-lived component
        wearDataLayerRegistry = WearDataLayerRegistry.fromContext(
            context ,
            serviceScope,
        ).apply {
           registerSerializer(WearDataSerializer)
           registerSerializer(TakenDataSerializer)
           registerSerializer(NotifyTakenDataSerializer)
        }
        appHelper = WearDataLayerAppHelper(context, wearDataLayerRegistry, serviceScope)
        serviceScope.launch {
            Log.i("WEAR", "Waiting")

            notifyTakenDataStore = wearDataLayerRegistry.protoDataStore(
                serviceScope
            )
//            wearDataLayerRegistry.protoFlow<WearData>(TargetNodeId.PairedPhone)
//                .collect { counterData -> Log.i("WEAR", "Counter: ${counterData.name}") }
        }


    }
    suspend fun connectedNodes(): List<AppHelperNodeStatus> {
        val connectedNodes = appHelper.connectedNodes()
        return connectedNodes
    }
    suspend fun recordMedicine(reminderEventId: Int, name: String, notificationId: Int) {
        notifyTakenDataStore.updateData { it.copy(name,reminderEventId,notificationId) }
    }
    fun close() {
        serviceScope.cancel()
    }
}
