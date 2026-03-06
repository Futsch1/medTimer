package com.futsch1.medtimer.helpers

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.futsch1.medtimer.LogTags
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.net.URLConnection
import java.nio.charset.StandardCharsets

object FileHelper {
    // Unencrypted file is intended here and not a mistake
    fun saveToFile(file: File, content: String): Boolean {
        try {
            FileWriter(file).use { writer ->
                writer.write(content)
            }
        } catch (e: IOException) {
            Log.e(LogTags.BACKUP, e.toString())
            return false
        }
        return true
    }

    fun readFromUri(uri: Uri?, resolver: ContentResolver): String? {
        if (uri != null && uri.path != null) {
            try {
                resolver.openInputStream(uri).use { inputStream ->
                    InputStreamReader(inputStream, StandardCharsets.UTF_8).use { inputStreamReader ->
                        val stringBuilder = getStringBuilder(inputStreamReader)
                        return stringBuilder.toString()
                    }
                }
            } catch (e: IOException) {
                Log.e(LogTags.BACKUP, e.toString())
            }
        }
        return null
    }

    @Throws(IOException::class)
    private fun getStringBuilder(inputStreamReader: InputStreamReader): StringBuilder {
        val stringBuilder = StringBuilder()
        BufferedReader(inputStreamReader).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                stringBuilder.append("\n")
                line = reader.readLine()
            }
        }
        return stringBuilder
    }


    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "com.futsch1.medtimer.fileprovider", file)
        val intentShareFile = Intent(Intent.ACTION_SEND)

        intentShareFile.setDataAndType(uri, URLConnection.guessContentTypeFromName(file.getName()))
        //Allow sharing apps to read the file Uri
        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        //Pass the file Uri instead of the path
        intentShareFile.putExtra(
            Intent.EXTRA_STREAM,
            uri
        )
        context.startActivity(Intent.createChooser(intentShareFile, "Share File"))
    }
}
