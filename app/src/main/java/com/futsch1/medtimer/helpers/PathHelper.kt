package com.futsch1.medtimer.helpers

import com.futsch1.medtimer.exporters.Export
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object PathHelper {
    private const val RESERVED_CHARS = "[\\[|?*<\":>+/'\\],]"

    fun getExportFilename(export: Export): String {
        val fileName = java.lang.String.format(
            "MedTimer_%sExport_%s.%s",
            export.type,
            ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
            export.extension
        )
        return fileName.replace(RESERVED_CHARS.toRegex(), "_")
    }

    val backupFilename: String
        get() {
            val fileName = String.format(
                "MedTimer_Backup_%s.json",
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            )
            return fileName.replace(RESERVED_CHARS.toRegex(), "_")
        }
}
