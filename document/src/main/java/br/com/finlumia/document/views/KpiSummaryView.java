package br.com.finlumia.document.views;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KpiSummaryView(
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldoLiquido,
        BigDecimal taxaPoupanca,
        BigDecimal patrimonioAtual,
        BigDecimal crescimentoPatrimonio,
        @JsonProperty("periodMonths") int periodMonths
) {
}
