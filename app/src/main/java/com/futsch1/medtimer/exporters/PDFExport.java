package com.futsch1.medtimer.exporters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.print.PrintAttributes;

import androidx.annotation.NonNull;

import com.futsch1.medtimer.R;
import com.futsch1.medtimer.database.ReminderEvent;
import com.wwdablu.soumya.simplypdf.SimplyPdf;
import com.wwdablu.soumya.simplypdf.SimplyPdfDocument;
import com.wwdablu.soumya.simplypdf.composers.properties.TableProperties;
import com.wwdablu.soumya.simplypdf.composers.properties.TextProperties;
import com.wwdablu.soumya.simplypdf.composers.properties.cell.Cell;
import com.wwdablu.soumya.simplypdf.composers.properties.cell.TextCell;
import com.wwdablu.soumya.simplypdf.document.DocumentInfo;
import com.wwdablu.soumya.simplypdf.document.Margin;
import com.wwdablu.soumya.simplypdf.document.PageHeader;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;

public class PDFExport implements Exporter {
    public static final String BLACK = "#000000";
    private final List<ReminderEvent> reminderEvents;
    private final Context context;
    private final ZoneId defaultZoneId;

    public PDFExport(List<ReminderEvent> reminderEvents, Context context, ZoneId zoneId) {
        this.reminderEvents = reminderEvents;
        this.context = context;
        this.defaultZoneId = zoneId;
    }


    public void export(File file) throws ExporterException {
        SimplyPdfDocument simplyPdfDocument = getDocument(file);

        LinkedList<LinkedList<Cell>> rows = new LinkedList<>();

        final int pageWidth = simplyPdfDocument.getUsablePageWidth();
        int[] columnWidths = {pageWidth / 4, pageWidth / 3, pageWidth / 4, pageWidth / 6};

        LinkedList<Cell> header = getHeader(columnWidths);
        rows.add(header);

        TextProperties textProperties = getTextProperties();
        for (ReminderEvent reminderEvent : reminderEvents) {
            LinkedList<Cell> row = getCells(reminderEvent, textProperties, columnWidths);
            rows.add(row);
        }

        simplyPdfDocument.getTable().draw(rows, getTableProperties());

        try {
            BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (scope, continuation) -> simplyPdfDocument.finish(continuation));
        } catch (InterruptedException e) {
            throw new ExporterException();
        }
    }

    @NonNull
    private SimplyPdfDocument getDocument(File file) {
        return SimplyPdf.with(this.context, file).colorMode(DocumentInfo.ColorMode.COLOR)
                .paperSize(PrintAttributes.MediaSize.ISO_A4)
                .margin(Margin.Companion.getDefault())
                .pageModifier(new PageHeader(new ArrayList<>()))
                .firstPageBackgroundColor(Color.WHITE)
                .paperOrientation(DocumentInfo.Orientation.PORTRAIT)
                .build();
    }

    @NonNull
    private LinkedList<Cell> getHeader(int[] columnWidths) {
        TextProperties headerProperties = getHeaderProperties();
        LinkedList<Cell> header = new LinkedList<>();
        int colIndex = 0;
        final int[] headerTexts = {R.string.time, R.string.medicine_name, R.string.dosage, R.string.taken};
        for (int headerText : headerTexts) {
            header.add(new TextCell(context.getString(headerText), headerProperties, columnWidths[colIndex++]));
        }
        return header;
    }

    @NonNull
    private static TextProperties getTextProperties() {
        TextProperties textProperties = new TextProperties();
        textProperties.textColor = BLACK;
        textProperties.textSize = 12;
        return textProperties;
    }

    @NonNull
    private LinkedList<Cell> getCells(ReminderEvent reminderEvent, TextProperties textProperties, int[] columnWidths) {
        ZonedDateTime zonedDateTime;
        Instant remindedTime;
        LinkedList<Cell> row = new LinkedList<>();
        remindedTime = Instant.ofEpochSecond(reminderEvent.remindedTimestamp);
        zonedDateTime = remindedTime.atZone(defaultZoneId);

        String time = String.format("%s %s",
                zonedDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
                zonedDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)));
        row.add(new TextCell(time, textProperties, columnWidths[0]));
        row.add(new TextCell(reminderEvent.medicineName, textProperties, columnWidths[1]));
        row.add(new TextCell(reminderEvent.amount, textProperties, columnWidths[2]));
        row.add(new TextCell(reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN ? "x" : "", textProperties, columnWidths[3]));
        return row;
    }

    @NonNull
    private static TableProperties getTableProperties() {
        TableProperties tableProperties = new TableProperties();
        tableProperties.borderColor = BLACK;
        tableProperties.borderWidth = 1;
        tableProperties.drawBorder = true;
        return tableProperties;
    }

    private static TextProperties getHeaderProperties() {
        TextProperties headerProperties = new TextProperties();
        headerProperties.textColor = BLACK;
        headerProperties.textSize = 14;
        headerProperties.typeface = Typeface.DEFAULT_BOLD;
        return headerProperties;
    }

    @Override
    public String getExtension() {
        return "pdf";
    }

}
