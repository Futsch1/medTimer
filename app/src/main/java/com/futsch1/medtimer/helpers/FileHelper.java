package com.futsch1.medtimer.helpers;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class FileHelper {
    private FileHelper() {
        // Intentionally empty
    }

    public static boolean saveToFile(File file, String content) {
        try {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }
        } catch (IOException e) {
            Log.e("FileHelper", e.toString());
            return false;
        }
        return true;
    }

    public static @Nullable String readFromUri(@Nullable Uri uri, ContentResolver resolver) {
        if (uri != null && uri.getPath() != null) {
            try (InputStream inputStream = resolver.openInputStream(uri)) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    StringBuilder stringBuilder = getStringBuilder(inputStreamReader);
                    return stringBuilder.toString();
                }
            } catch (IOException e) {
                Log.e("FileHelper", e.toString());
            }
        }
        return null;
    }

    @NonNull
    private static StringBuilder getStringBuilder(InputStreamReader inputStreamReader) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
                line = reader.readLine();
            }
        }
        return stringBuilder;
    }


    public static void shareFile(Context context, File file) {
        Uri uri = FileProvider.getUriForFile(context, "com.futsch1.medtimer.fileprovider", file);
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);

        intentShareFile.setDataAndType(uri, URLConnection.guessContentTypeFromName(file.getName()));
        //Allow sharing apps to read the file Uri
        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //Pass the file Uri instead of the path
        intentShareFile.putExtra(Intent.EXTRA_STREAM,
                uri);
        context.startActivity(Intent.createChooser(intentShareFile, "Share File"));
    }
}
