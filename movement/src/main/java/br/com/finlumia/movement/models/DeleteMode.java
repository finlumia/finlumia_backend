package br.com.finlumia.movement.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeleteMode {
    SINGLE("single"),
    FROM_HERE("from_here"),
    ALL("all");

    private final String value;

    DeleteMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DeleteMode fromValue(String value) {
        for (DeleteMode d : values()) {
            if (d.value.equalsIgnoreCase(value)) return d;
        }
        throw new IllegalArgumentException("DeleteMode inválido: " + value);
    }
}
