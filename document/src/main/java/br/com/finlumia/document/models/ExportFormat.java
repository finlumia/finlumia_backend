package br.com.finlumia.document.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExportFormat {
    PDF("pdf"),
    CSV("csv"),
    XLSX("xlsx");

    private final String value;

    ExportFormat(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ExportFormat fromValue(String value) {
        for (ExportFormat f : values()) {
            if (f.value.equalsIgnoreCase(value)) return f;
        }
        throw new IllegalArgumentException("ExportFormat inválido: " + value);
    }
}
