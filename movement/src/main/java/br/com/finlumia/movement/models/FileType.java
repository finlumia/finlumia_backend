package br.com.finlumia.movement.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FileType {
    OFX("ofx"),
    CSV("csv"),
    IMAGE("image");

    private final String value;

    FileType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static FileType fromValue(String value) {
        for (FileType f : values()) {
            if (f.value.equalsIgnoreCase(value)) return f;
        }
        throw new IllegalArgumentException("FileType inválido: " + value);
    }

    public static FileType fromContentType(String originalFilename) {
        if (originalFilename == null) return IMAGE;
        String lower = originalFilename.toLowerCase();
        if (lower.endsWith(".ofx")) return OFX;
        if (lower.endsWith(".csv")) return CSV;
        return IMAGE;
    }
}
