package com.futsch1.medtimer.exporters

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.TableHelper.getTableHeadersForEventExport
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.helpers.TimeHelper.secondsSinceEpochToISO8601DatetimeString
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException

class CSVEventExport @AssistedInject constructor(
    @Assisted private val reminderEvents: List<ReminderEvent>,
    @Assisted fragmentManager: FragmentManager,
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val timeFormatter: TimeFormatter
) : Export(fragmentManager) {

    @AssistedFactory
    fun interface Factory {
        fun create(reminderEvents: List<ReminderEvent>, fragmentManager: FragmentManager): CSVEventExport
    }

    @Throws(ExporterException::class)  // Unencrypted file is intended here and not a mistake. We need the \n linebreak explicitly
    public override suspend fun exportInternal(file: File) {
        try {
            withContext(ioDispatcher) {
                FileWriter(file).use { csvFile ->
                    val headerTexts: List<String> = getTableHeadersForEventExport(context)
                    csvFile.write(headerTexts.joinToString(";") + "\n")
                    for (reminderEvent in reminderEvents) {
                        val line = String.format(
                            "%s;%s;%s;%s;%s;%s;%s;%s;%s\n",
                            timeFormatter.secondsSinceEpochToDateTimeString(
                                reminderEvent.remindedTimestamp
                            ),
                            reminderEvent.medicineName,
                            reminderEvent.amount,
                            if (reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN) timeFormatter.secondsSinceEpochToDateTimeString(
                                reminderEvent.processedTimestamp
                            ) else "",
                            reminderEvent.tags.joinToString(", "),
                            timeFormatter.minutesToDurationString(reminderEvent.lastIntervalReminderTimeInMinutes.toLong()),
                            reminderEvent.notes,
                            secondsSinceEpochToISO8601DatetimeString(reminderEvent.remindedTimestamp),
                            if (reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN) secondsSinceEpochToISO8601DatetimeString(
                                reminderEvent.processedTimestamp
                            ) else ""
                        )
                        csvFile.write(line)
                    }
                }
            }
        } catch (_: IOException) {
            throw ExporterException()
        }
    }

    override val extension = "csv"
    override val type = "Events"
}
