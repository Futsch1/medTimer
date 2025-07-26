package com.futsch1.medtimer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.core.DataStore
import com.futsch1.medtimer.reminders.ReminderProcessor
import com.futsch1.medtimer.reminders.RescheduleWork
import com.futsch1.medtimer.shared.wear.NotifyTakenData
import com.futsch1.medtimer.shared.wear.NotifyTakenDataSerializer
import com.futsch1.medtimer.shared.wear.TakenData
import com.futsch1.medtimer.shared.wear.TakenDataSerializer
import com.futsch1.medtimer.shared.wear.WearData
import com.futsch1.medtimer.shared.wear.WearDataSerializer
import com.futsch1.medtimer.wear.createRemoteReminderNotificationData
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Wearable
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ActivityConfig
import com.google.android.horologist.data.AppHelperResult
import com.google.android.horologist.data.AppHelperResultCode
import com.google.android.horologist.data.ProtoDataStoreHelper.protoDataStore
import com.google.android.horologist.data.ProtoDataStoreHelper.protoFlow
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.activityConfig
import com.google.android.horologist.data.apphelper.AppHelperNodeStatus
import com.google.android.horologist.data.apphelper.DataLayerAppHelper.Companion.LAUNCH_APP
import com.google.android.horologist.data.apphelper.DataLayerAppHelper.Companion.MESSAGE_REQUEST_TIMEOUT_MS
import com.google.android.horologist.data.launchRequest
import com.google.android.horologist.datalayer.phone.PhoneDataLayerAppHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalHorologistApi::class, DelicateCoroutinesApi::class)
class PhoneWearModule(val context: Context) {
    private lateinit var wearDataLayerRegistry: WearDataLayerRegistry
    private lateinit var appHelper: PhoneDataLayerAppHelper

    val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private lateinit var wearDataStore: DataStore<WearData>
    private lateinit var takenDataStore: DataStore<TakenData>

    fun notifyTakenData(): Flow<NotifyTakenData> =
        wearDataLayerRegistry.protoFlow(PairedWatch)

    private object PairedWatch : TargetNodeId {
        override suspend fun evaluate(dataLayerRegistry: WearDataLayerRegistry): String? {
            val capabilitySearch = dataLayerRegistry.capabilityClient.getCapability(
                "horologist_watch",
                CapabilityClient.FILTER_ALL,
            ).await()

            return capabilitySearch.nodes.singleOrNull()?.id
        }
    }
    init {
        if (isInstalled()) {
            wearDataLayerRegistry = WearDataLayerRegistry.fromContext(
                context,
                serviceScope,
            )
            wearDataLayerRegistry.apply {
                registerSerializer(WearDataSerializer)
                registerSerializer(TakenDataSerializer)
                registerSerializer(NotifyTakenDataSerializer)
            }
            appHelper = PhoneDataLayerAppHelper(context, wearDataLayerRegistry)
        }
    }
    private fun isInstalled() : Boolean{
          try {
              val capabilityInfo: CapabilityInfo = Tasks.await(
                  Wearable.getCapabilityClient(context)
                      .getCapability(
                          "horologist_watch",
                          CapabilityClient.FILTER_REACHABLE
                      )
              )
              return true;
          } catch (e: Exception) {
              e.message?.run {
                  if (contains("API_UNAVAILABLE")) {
                      Log.e("WEAR", "wear app is not installed")
                      //TODO make User install wear app
                      return false
                  }
              }
          }
          return false
      }

    suspend fun connectedNodes(): List<AppHelperNodeStatus> {
        val connectedNodes = appHelper.connectedNodes()
        return connectedNodes
    }
    suspend fun isAvailable(): Boolean {
        return appHelper.isAvailable()
    }

    suspend fun startCompanion(reminderEventId:Int, name:String, notificationId:Int) {
        if (!isInstalled()) return ;
        if (!this::takenDataStore.isInitialized) {
            takenDataStore = wearDataLayerRegistry.protoDataStore(
                serviceScope
            )
        }
        takenDataStore.updateData { TakenData(name = name, reminderEventId = reminderEventId, active = true, notificationId = notificationId) }
        val connectedNodes = appHelper.connectedNodes()
        startService()
        for (node in connectedNodes) {
            Log.i("WEAR", "node: ${node}")
            val config = activityConfig {
                classFullName = "com.futsch1.medtimer.presentation.TakenActivity"
            }
            val result = startRemoteActivity(node, config)
            Log.i("WEAR", "result: ${result}")
        }
    }

    private fun startService() {
        try {

            Intent(context, ReminderSchedulerService::class.java).also {
                    context.startForegroundService(it)
                    return
            }
        }
        catch (e: Exception){
            Log.e("WEAR", "error on startService: ${e}")
        }

    }

    suspend fun checkNotifications() {
        if (!isInstalled()) return ;
        serviceScope.launch {
            try {
                notifyTakenData().collect { notifyTakenData ->
                    Log.i("WEAR", "notifyTakenData: ${notifyTakenData}")
                    val notifyTaken = ReminderProcessor.getTakenActionIntent(
                        context,
                        notifyTakenData.reminderEventId
                    )
                    val notificatuion = PendingIntent.getBroadcast(
                        context,
                        notifyTakenData.notificationId,
                        notifyTaken,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    notificatuion.send()
                }
            } catch (e: Exception) {
                Log.e("WEAR", "error on notifyTakenData: ${e}")
            }
        }
    }

    suspend fun addReminderNotificationData(reminderNotificationData: RescheduleWork.ReminderNotificationData?): Unit {
        if (!isInstalled()) return ;
        if (!this::wearDataStore.isInitialized) {
            wearDataStore = wearDataLayerRegistry.protoDataStore(
                serviceScope
            )
        }
        serviceScope.launch {
            wearDataStore.updateData {
                WearData(
                    name = Math.random().toString(),
                    reminderNotificationData = createRemoteReminderNotificationData(
                        reminderNotificationData
                    )
                )
            }
        }
    }

    fun close() {
        Log.i("WEAR", "close")
        try {
            serviceScope.cancel()
        }
        catch (e: Exception){}
    }


    private suspend fun startRemoteActivity(
        node: AppHelperNodeStatus,
        config: ActivityConfig
    ): AppHelperResultCode {
        val request = launchRequest { activity = config }
        val result = sendRequestWithTimeout(node.id, LAUNCH_APP, request.toByteArray())
        return result
    }
    private suspend fun sendRequestWithTimeout(
        nodeId: String,
        path: String,
        data: ByteArray,
        timeoutMs: Long = MESSAGE_REQUEST_TIMEOUT_MS,
    ): AppHelperResultCode {
        val response = try {
            withTimeout(timeoutMs) {
                // Cancellation will not lead to the GMS Task itself being cancelled.
                wearDataLayerRegistry.messageClient.sendRequest(nodeId, path, data).await()
            }
        } catch (timeoutException: TimeoutCancellationException) {
            return AppHelperResultCode.APP_HELPER_RESULT_TIMEOUT
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.TIMEOUT) {
                return AppHelperResultCode.APP_HELPER_RESULT_TIMEOUT
            } else {
                throw e
            }
        }
        return AppHelperResult.parseFrom(response).code
    }
}
