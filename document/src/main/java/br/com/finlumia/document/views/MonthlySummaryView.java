package br.com.finlumia.document.views;

import java.math.BigDecimal;

public record MonthlySummaryView(
        String month,
        int year,
        BigDecimal receitas,
        BigDecimal despesas,
        BigDecimal saldo,
        BigDecimal patrimonio
) {
}
