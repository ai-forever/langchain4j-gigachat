package ru.sber.sbe.gigachat;

import com.google.gson.*;

/**
 * @author protas-nv 06.02.2025
 */
public final class Utils {

    public static final Gson gson = new Gson();

    private Utils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static <T> T defaultIfNull(T object, T defaultValue) {
        return object != null ? object : defaultValue;
    }

    public static String minifyJson(String str) {
        try {
            JsonElement jsonElement = JsonParser.parseString(str);
            return gson.toJson(jsonElement);
        } catch (Exception __) {
            return str;
        }
    }
}