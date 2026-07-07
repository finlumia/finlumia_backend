package br.com.finlumia.movement.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SortBy {
    DATE("date"),
    AMOUNT("amount"),
    DESCRIPTION("description"),
    CATEGORY("category");

    private final String value;

    SortBy(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String toColumn() {
        return value;
    }

    @JsonCreator
    public static SortBy fromValue(String value) {
        for (SortBy s : values()) {
            if (s.value.equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("SortBy inválido: " + value);
    }
}
