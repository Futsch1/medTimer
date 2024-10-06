package com.futsch1.medtimer.database;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

public abstract class JSONBackup<T> {

    private final Class<T> contentClass;

    protected JSONBackup(Class<T> contentClass) {
        this.contentClass = contentClass;
    }

    public String createBackupAsString(int databaseVersion, List<T> list) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(createBackup(databaseVersion, list));
    }

    public JsonElement createBackup(int databaseVersion, List<T> list) {
        JSONBackup.DatabaseContentWithVersion<T> content = new JSONBackup.DatabaseContentWithVersion<>(databaseVersion, list);
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        return gson.toJsonTree(content);
    }

    public @Nullable List<T> parseBackup(String jsonFile) {
        // In a first step, parse with the version set to 0
        Gson gson = registerTypeAdapters(new GsonBuilder()).setVersion(0.0).create();
        try {
            Type type = TypeToken.getParameterized(DatabaseContentWithVersion.class, contentClass).getType();
            DatabaseContentWithVersion<T> content = gson.fromJson(jsonFile, type);
            if (content != null && content.version >= 0) {
                gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setVersion(content.version).create();
                content = gson.fromJson(jsonFile, type);
                return checkBackup(content.list);
            }
            return null;
        } catch (JsonParseException e) {
            Log.e("JSONBackup", e.getMessage() != null ? e.getMessage() : "");
            return null;
        }
    }

    protected abstract GsonBuilder registerTypeAdapters(GsonBuilder builder);

    private @Nullable List<T> checkBackup(List<T> list) {
        if (list != null) {
            for (T item : list) {
                if (isInvalid(item)) return null;
            }
        }
        return list;
    }

    protected abstract boolean isInvalid(T item);

    public abstract void applyBackup(List<T> list, MedicineRepository medicineRepository);

    protected record DatabaseContentWithVersion<T>(@Expose int version,
                                                   @Expose List<T> list) {
    }

    protected static class FullDeserialize<T> implements JsonDeserializer<T> {

        public T deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
            T pojo = new Gson().fromJson(je, type);

            Field[] fields = pojo.getClass().getDeclaredFields();
            for (Field f : fields) {
                try {
                    if (f.get(pojo) == null) {
                        throw new JsonParseException("Missing field in JSON: " + f.getName());
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Log.e("JSONBackup", "Internal error");
                }

            }
            return pojo;

        }
    }
}
