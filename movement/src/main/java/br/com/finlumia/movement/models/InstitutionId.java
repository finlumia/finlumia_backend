package br.com.finlumia.movement.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum InstitutionId {
    NUBANK("nubank"),
    ITAU("itau"),
    BB("bb"),
    BRADESCO("bradesco"),
    SANTANDER("santander"),
    PICPAY("picpay"),
    INTER("inter"),
    C6("c6"),
    XP("xp");

    private final String value;

    InstitutionId(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static InstitutionId fromValue(String value) {
        for (InstitutionId i : values()) {
            if (i.value.equalsIgnoreCase(value)) return i;
        }
        throw new IllegalArgumentException("InstitutionId inválido: " + value);
    }
}
