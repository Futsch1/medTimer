package com.futsch1.medtimer;

import android.content.Context;
import android.graphics.Color;
import android.print.PrintAttributes;

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
import java.io.IOException;
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

public class PDFCreator {
    private final List<ReminderEvent> reminderEvents;
    private final Context context;
    private final ZoneId defaultZoneId;

    public PDFCreator(List<ReminderEvent> reminderEvents, Context context, ZoneId zoneId) {
        this.reminderEvents = reminderEvents;
        this.context = context;
        this.defaultZoneId = zoneId;
    }

    public void create(File file) throws IOException {
        SimplyPdfDocument simplyPdfDocument = SimplyPdf.with(this.context, file)
                .colorMode(DocumentInfo.ColorMode.COLOR)
                .paperSize(PrintAttributes.MediaSize.ISO_A4)
                .margin(Margin.Companion.getDefault())
                .pageModifier(new PageHeader(new ArrayList<>()))
                .firstPageBackgroundColor(Color.WHITE)
                .paperOrientation(DocumentInfo.Orientation.PORTRAIT)
                .build();

        TableProperties tableProperties = new TableProperties();
        tableProperties.borderColor = "#000000";
        tableProperties.borderWidth = 1;
        tableProperties.drawBorder = true;

        TextProperties textProperties = new TextProperties();
        textProperties.textColor = "#000000";
        textProperties.textSize = 12;

        LinkedList<LinkedList<Cell>> rows = new LinkedList<>();
        // Create header
        LinkedList<Cell> header = new LinkedList<>();
        final int[] headerTexts = {R.string.time, R.string.medicine_name, R.string.dosage, R.string.taken};

        for (int headerText : headerTexts) {
            header.add(new TextCell(context.getString(headerText), textProperties, simplyPdfDocument.getUsablePageWidth()));
        }
        rows.add(header);

        Instant remindedTime;
        ZonedDateTime zonedDateTime;
        for (ReminderEvent reminderEvent : reminderEvents) {
            LinkedList<Cell> row = new LinkedList<>();
            remindedTime = Instant.ofEpochSecond(reminderEvent.remindedTimestamp);
            zonedDateTime = remindedTime.atZone(defaultZoneId);

            String time = String.format("%s %s",
                    zonedDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)),
                    zonedDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)));
            row.add(new TextCell(time, textProperties, simplyPdfDocument.getUsablePageWidth()));
            row.add(new TextCell(reminderEvent.medicineName, textProperties, simplyPdfDocument.getUsablePageWidth()));
            row.add(new TextCell(reminderEvent.amount, textProperties, simplyPdfDocument.getUsablePageWidth()));
            row.add(new TextCell(reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN ? "x" : "", textProperties, simplyPdfDocument.getUsablePageWidth()));
            rows.add(row);
        }

        simplyPdfDocument.getTable().draw(rows, tableProperties);

        try {
            BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, (scope, continuation) -> simplyPdfDocument.finish(continuation));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
