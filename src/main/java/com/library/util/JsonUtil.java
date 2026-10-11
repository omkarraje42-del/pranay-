package com.library.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class JsonUtil {

    private static final Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder();

        // LocalDate adapter
        builder.registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
            @Override
            public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
        });
        builder.registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json == null || json.isJsonNull() || json.getAsString().trim().isEmpty()) {
                    return null;
                }
                return LocalDate.parse(json.getAsString().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            }
        });

        // OffsetDateTime adapter
        builder.registerTypeAdapter(OffsetDateTime.class, new JsonSerializer<OffsetDateTime>() {
            @Override
            public JsonElement serialize(OffsetDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
        });
        builder.registerTypeAdapter(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json == null || json.isJsonNull() || json.getAsString().trim().isEmpty()) {
                    return null;
                }
                return OffsetDateTime.parse(json.getAsString().trim(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }
        });

        gson = builder.setPrettyPrinting().create();
    }

    public static Gson getGson() {
        return gson;
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public static <T> T fromJson(String json, Type typeOfT) {
        return gson.fromJson(json, typeOfT);
    }
}
