package com.example.bestapplication.core.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Small JSON helpers used across the UI and scan pipelines.
 */
public final class JsonUtils {

    private JsonUtils() {}

    public static JsonObject safeParseObject(String json) {
        try {
            if (json == null) return new JsonObject();
            String s = json.trim();
            if (s.isEmpty() || "{}".equals(s)) return new JsonObject();

            JsonElement el = JsonParser.parseString(s);
            return (el != null && el.isJsonObject()) ? el.getAsJsonObject() : new JsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    public static String getString(JsonObject obj, String key) {
        try {
            if (obj == null || key == null) return "";
            JsonElement e = obj.get(key);
            if (e == null || e.isJsonNull()) return "";
            return e.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    public static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String nvl(String s) {
        return s == null ? "" : s.trim();
    }
}
