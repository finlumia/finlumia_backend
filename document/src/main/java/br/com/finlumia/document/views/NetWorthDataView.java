package br.com.finlumia.document.views;

import java.math.BigDecimal;

public record NetWorthDataView(
        String month,
        int year,
        BigDecimal patrimonio
) {
}
