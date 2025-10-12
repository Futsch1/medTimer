package com.futsch1.medtimer.exporters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.print.PrintAttributes
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.reminderSummary
import com.wwdablu.soumya.simplypdf.SimplyPdf
import com.wwdablu.soumya.simplypdf.SimplyPdfDocument
import com.wwdablu.soumya.simplypdf.composers.properties.TextProperties
import com.wwdablu.soumya.simplypdf.document.DocumentInfo
import com.wwdablu.soumya.simplypdf.document.DocumentInfo.ColorMode
import com.wwdablu.soumya.simplypdf.document.Margin.Companion.default
import com.wwdablu.soumya.simplypdf.document.PageHeader
import java.io.File

const val BLACK: String = "#000000"

val standardTextProperties = TextProperties().apply()
{
    textColor = BLACK
    textSize = 12
}

val bulletTextProperties = TextProperties().apply()
{
    textColor = BLACK
    textSize = 12
    bulletSymbol = "•"
}

val biggerBoldProperties = TextProperties().apply()
{
    textColor = BLACK
    textSize = 14
    typeface = Typeface.DEFAULT_BOLD
}

val biggestBoldProperties = TextProperties().apply()
{
    textColor = BLACK
    textSize = 16
    typeface = Typeface.DEFAULT_BOLD
}

fun getDocument(context: Context, file: File): SimplyPdfDocument {
    return SimplyPdf.with(context, file).colorMode(ColorMode.COLOR)
        .paperSize(PrintAttributes.MediaSize.ISO_A4)
        .margin(default)
        .pageModifier(PageHeader(ArrayList()))
        .firstPageBackgroundColor(Color.WHITE)
        .paperOrientation(DocumentInfo.Orientation.PORTRAIT)
        .build()
}

fun getExportReminderSummary(context: Context, reminder: Reminder): String {
    var summary = ""
    if (reminder.reminderType == Reminder.ReminderType.TIME_BASED) {
        summary = TimeHelper.minutesToTimeString(context, reminder.timeInMinutes.toLong()) + ", "
    }
    return summary + reminderSummary(reminder, context)
}