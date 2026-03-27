package com.futsch1.medtimer.exporters

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.TableHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.wwdablu.soumya.simplypdf.composers.properties.TableProperties
import com.wwdablu.soumya.simplypdf.composers.properties.TextProperties
import com.wwdablu.soumya.simplypdf.composers.properties.cell.Cell
import com.wwdablu.soumya.simplypdf.composers.properties.cell.TextCell
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.LinkedList
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class PDFEventExport @AssistedInject constructor(
    @Assisted private val reminderEvents: List<ReminderEvent>,
    @Assisted fragmentManager: FragmentManager,
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : Export(fragmentManager) {

    @AssistedFactory
    fun interface Factory {
        fun create(reminderEvents: List<ReminderEvent>, fragmentManager: FragmentManager): PDFEventExport
    }

    private val tableProperties = TableProperties().apply()
    {
        borderColor = BLACK
        borderWidth = 1
        drawBorder = true
    }

    @OptIn(ExperimentalTime::class)
    public override suspend fun exportInternal(file: File) {
        val simplyPdfDocument = getDocument(context, file)

        val rows = LinkedList<LinkedList<Cell>>()

        val pageWidth = simplyPdfDocument.usablePageWidth
        val columnWidths = intArrayOf(pageWidth / 4, pageWidth / 3, pageWidth / 6, pageWidth / 4)

        val header = getHeader(columnWidths)
        rows.add(header)

        for (reminderEvent in reminderEvents) {
            val row = getCells(reminderEvent, standardTextProperties, columnWidths)
            rows.add(row)
        }

        simplyPdfDocument.text.write(TimeHelper.secondsSinceEpochToDateTimeString(context, Clock.System.now().epochSeconds) + "\n", standardTextProperties)
        simplyPdfDocument.table.draw(rows, tableProperties)

        withContext(ioDispatcher) {
            simplyPdfDocument.finish()
        }
    }

    private fun getHeader(columnWidths: IntArray): LinkedList<Cell> {
        val header = LinkedList<Cell>()
        var colIndex = 0
        for (headerText in TableHelper.getTableHeadersForEventExport(context)) {
            if (colIndex >= columnWidths.size) {
                break
            }
            header.add(TextCell(headerText, biggerBoldProperties, columnWidths[colIndex++]))
        }
        return header
    }

    private fun getCells(reminderEvent: ReminderEvent, textProperties: TextProperties, columnWidths: IntArray): LinkedList<Cell> {
        val row = LinkedList<Cell>()
        row.add(TextCell(TimeHelper.secondsSinceEpochToDateTimeString(context, reminderEvent.remindedTimestamp), textProperties, columnWidths[0]))
        row.add(TextCell(reminderEvent.medicineName, textProperties, columnWidths[1]))
        row.add(TextCell(reminderEvent.amount, textProperties, columnWidths[2]))
        row.add(
            TextCell(
                if (reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN) TimeHelper.secondsSinceEpochToDateTimeString(
                    context,
                    reminderEvent.processedTimestamp
                ) else "", textProperties, columnWidths[3]
            )
        )
        return row
    }

    override val extension = "pdf"
    override val type = "Events"
}
