package br.com.finlumia.document.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportSection {
    KPIS("kpis"),
    CASH_FLOW("cash_flow"),
    BY_CATEGORY("by_category"),
    NET_WORTH("net_worth"),
    INSIGHTS("insights");

    private final String value;

    ReportSection(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReportSection fromValue(String value) {
        for (ReportSection s : values()) {
            if (s.value.equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("ReportSection inválido: " + value);
    }
}
