package br.com.finlumia.movement.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CategoryId {
    ALIMENTACAO("alimentacao"),
    SAUDE("saude"),
    EDUCACAO("educacao"),
    TRANSPORTE("transporte"),
    LAZER("lazer"),
    MORADIA("moradia"),
    SALARIO("salario"),
    VENDAS("vendas"),
    TECNOLOGIA("tecnologia"),
    MARKETING("marketing"),
    SERVICOS("servicos"),
    INVESTIMENTO("investimento"),
    OUTROS("outros");

    private final String value;

    CategoryId(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CategoryId fromValue(String value) {
        for (CategoryId c : values()) {
            if (c.value.equalsIgnoreCase(value)) return c;
        }
        throw new IllegalArgumentException("CategoryId inválido: " + value);
    }
}
