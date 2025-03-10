package ru.sber.sbe.gigachat.internal.api.model.completions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FinishReasonEnum {

    LENGTH("length"),

    STOP("stop"),

    FUNCTION_CALL("function_call"),

    BLACKLIST("blacklist");

    private final String value;

    FinishReasonEnum(String value) {
        this.value = value;
    }

    @JsonCreator
    public static FinishReasonEnum fromValue(String value) {
        for (FinishReasonEnum b : FinishReasonEnum.values()) {
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
