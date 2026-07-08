package com.futsch1.medtimer.feature.reminders.wear

import android.content.Context
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Pushes a snapshot of today's dose reminders to the watch whenever they change, reusing the same
 * "is this on today" filtering [TimeHelper.isOnDay] that the phone's Overview screen uses
 * (`OverviewViewModel.isReminderEventVisible`) - kept in sync by hand since that logic lives in
 * `:feature:ui`, which this module doesn't depend on.
 */
class GmsWearSyncController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val reminderEventRepository: ReminderEventRepository,
    private val reminderRepository: ReminderRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) : WearSyncController {

    private val gson = Gson()
    private val zone: ZoneId = ZoneId.systemDefault()
    private val stockEventTypes = setOf(ReminderType.OUT_OF_STOCK, ReminderType.EXPIRATION_DATE, ReminderType.REFILL)

    override fun start() {
        val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant()

        applicationScope.launch {
            reminderEventRepository.getAllFlow(startOfToday, ReminderEvent.statusValuesWithoutDelete)
                .collect { events ->
                    val today = events
                        .filter { it.reminderType !in stockEventTypes }
                        .filter { TimeHelper.isOnDay(it.remindedTimestamp.epochSecond, LocalDate.now(zone).toEpochDay(), zone) }

                    val variableAmountByReminderId = today.map { it.reminderId }.distinct()
                        .associateWith { reminderRepository.fetch(it)?.variableAmount == true }

                    push(today.map { it.toWatchReminderItem(variableAmountByReminderId) })
                }
        }
    }

    private fun ReminderEvent.toWatchReminderItem(variableAmountByReminderId: Map<Int, Boolean>): WatchReminderItem {
        return WatchReminderItem(
            reminderEventId = reminderEventId,
            reminderId = reminderId,
            medicineName = medicineName,
            amount = amount,
            remindedEpochSecond = remindedTimestamp.epochSecond,
            status = status.name,
            variableAmount = variableAmountByReminderId[reminderId] == true
        )
    }

    private suspend fun push(items: List<WatchReminderItem>) {
        val request = PutDataMapRequest.create(WearProtocol.TODAY_DATA_PATH).apply {
            dataMap.putString(WearProtocol.TODAY_DATA_KEY, gson.toJson(items))
            dataMap.putLong("pushedAt", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        runCatching { Wearable.getDataClient(context).putDataItem(request).await() }
    }
}
