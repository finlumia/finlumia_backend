package br.com.finlumia.document.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum InsightType {
    POSITIVE("positive"),
    NEGATIVE("negative"),
    NEUTRAL("neutral"),
    ALERT("alert");

    private final String value;

    InsightType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static InsightType fromValue(String value) {
        for (InsightType t : values()) {
            if (t.value.equalsIgnoreCase(value)) return t;
        }
        throw new IllegalArgumentException("InsightType inválido: " + value);
    }
}
