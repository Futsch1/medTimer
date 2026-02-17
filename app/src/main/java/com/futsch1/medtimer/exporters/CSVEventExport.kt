package com.futsch1.medtimer.exporters

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.TableHelper.getTableHeadersForEventExport
import com.futsch1.medtimer.helpers.TimeHelper.minutesToDurationString
import com.futsch1.medtimer.helpers.TimeHelper.secondsSinceEpochToDateTimeString
import com.futsch1.medtimer.helpers.TimeHelper.secondsSinceEpochToISO8601DatetimeString
import java.io.File
import java.io.FileWriter
import java.io.IOException

class CSVEventExport(private val reminderEvents: List<ReminderEvent>, fragmentManager: FragmentManager, private val context: Context) :
    Export(fragmentManager) {

    @Throws(ExporterException::class)  // Unencrypted file is intended here and not a mistake. We need the \n linebreak explicitly
    public override fun exportInternal(file: File) {
        try {
            FileWriter(file).use { csvFile ->
                val headerTexts: List<String> = getTableHeadersForEventExport(context)
                csvFile.write(headerTexts.joinToString(";") + "\n")
                for (reminderEvent in reminderEvents) {
                    val line = String.format(
                        "%s;%s;%s;%s;%s;%s;%s;%s;%s\n",
                        secondsSinceEpochToDateTimeString(context, reminderEvent.remindedTimestamp),
                        reminderEvent.medicineName,
                        reminderEvent.amount,
                        if (reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN) secondsSinceEpochToDateTimeString(
                            context,
                            reminderEvent.processedTimestamp
                        ) else "",
                        reminderEvent.tags.joinToString(", "),
                        minutesToDurationString(reminderEvent.lastIntervalReminderTimeInMinutes.toLong()),
                        reminderEvent.notes,
                        secondsSinceEpochToISO8601DatetimeString(reminderEvent.remindedTimestamp),
                        if (reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN) secondsSinceEpochToISO8601DatetimeString(reminderEvent.processedTimestamp) else ""
                    )
                    csvFile.write(line)
                }
            }
        } catch (_: IOException) {
            throw ExporterException()
        }
    }

    override val extension = "csv"
    override val type = "Events"
}
