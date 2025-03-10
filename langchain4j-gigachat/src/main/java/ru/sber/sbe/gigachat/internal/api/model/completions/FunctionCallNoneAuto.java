package ru.sber.sbe.gigachat.internal.api.model.completions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FunctionCallNoneAuto {
    AUTO("auto"),
    NONE("none");

    private final String value;

    FunctionCallNoneAuto(String value) {
        this.value = value;
    }

    @JsonCreator
    public static FunctionCallNoneAuto fromValue(String value) {
        for (FunctionCallNoneAuto b : FunctionCallNoneAuto.values()) {
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
