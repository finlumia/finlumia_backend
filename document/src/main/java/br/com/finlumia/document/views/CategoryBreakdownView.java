package br.com.finlumia.document.views;

import java.math.BigDecimal;

public record CategoryBreakdownView(
        String categoryId,
        String label,
        String color,
        BigDecimal total,
        BigDecimal percent,
        BigDecimal trend,
        int transactions
) {
}
