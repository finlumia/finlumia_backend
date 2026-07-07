package br.com.finlumia.document.views;

import java.math.BigDecimal;

public record InstitutionBreakdownView(
        String id,
        String label,
        String color,
        String abbr,
        BigDecimal total,
        BigDecimal percent
) {
}
