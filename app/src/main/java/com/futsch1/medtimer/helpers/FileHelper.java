package com.futsch1.medtimer.helpers;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

    public static @Nullable String readFromUri(@Nullable Uri uri) {
        if (uri != null && uri.getPath() != null) {
            File file = new File(uri.getPath());

            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
                List<String> lines = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                    String line = reader.readLine();
                    while (line != null) {
                        lines.add(line);
                        line = reader.readLine();
                    }
                }
                return String.join("\n", lines);
            } catch (IOException e) {
                Log.e("FileHelper", e.toString());
            }

        }
        return null;
    }
}
