package ru.sber.sbe.gigachat.internal.api.model.completions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ModelEnum {
    GIGA_CHAT("GigaChat"),
    GIGA_CHAT_PLUS("GigaChat-Plus"),
    GIGA_CHAT_PRO("GigaChat-Pro"),
    GIGA_CHAT_MAX("GigaChat-Max");
    private final String value;

    ModelEnum(String value) {
        this.value = value;
    }

    @JsonCreator
    public static ModelEnum fromValue(String value) {
        for (ModelEnum b : ModelEnum.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}