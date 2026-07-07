package br.com.finlumia.document.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Period {
    THREE_MONTHS("3m"),
    SIX_MONTHS("6m"),
    TWELVE_MONTHS("12m"),
    YTD("ytd"),
    CUSTOM("custom");

    private final String value;

    Period(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Period fromValue(String value) {
        for (Period p : values()) {
            if (p.value.equalsIgnoreCase(value)) return p;
        }
        throw new IllegalArgumentException("Period inválido: " + value);
    }
}
