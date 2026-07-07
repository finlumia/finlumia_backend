package br.com.finlumia.movement.views;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TotalsView(
        @JsonProperty("totalIncome") BigDecimal totalIncome,
        @JsonProperty("totalExpenses") BigDecimal totalExpenses,
        @JsonProperty("netBalance") BigDecimal netBalance
) {}
