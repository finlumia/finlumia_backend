package br.com.finlumia.movement.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BudgetScope {
    GERAL("geral"),
    CATEGORIA("categoria"),
    FORMA_PAGAMENTO("forma_pagamento"),
    BANCO("banco");

    private final String value;

    BudgetScope(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BudgetScope fromValue(String value) {
        for (BudgetScope s : values()) {
            if (s.value.equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("BudgetScope inválido: " + value);
    }
}
