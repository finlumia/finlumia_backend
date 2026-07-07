package br.com.finlumia.movement.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentMethod {
    PIX("pix"),
    CREDITO("credito"),
    DEBITO("debito"),
    DINHEIRO("dinheiro"),
    TED("ted"),
    DOC("doc");

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PaymentMethod fromValue(String value) {
        for (PaymentMethod m : values()) {
            if (m.value.equalsIgnoreCase(value)) return m;
        }
        throw new IllegalArgumentException("PaymentMethod inválido: " + value);
    }
}
