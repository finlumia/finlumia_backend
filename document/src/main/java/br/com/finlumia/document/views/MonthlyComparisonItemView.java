package br.com.finlumia.document.views;

import java.math.BigDecimal;

public record MonthlyComparisonItemView(
        String label,
        BigDecimal receitas,
        BigDecimal despesas,
        BigDecimal saldo
) {
}
