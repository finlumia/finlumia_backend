package br.com.finlumia.document.views;

import java.math.BigDecimal;
import java.util.List;

public record InstitutionBreakdownListView(
        List<InstitutionBreakdownView> data,
        BigDecimal total
) {
}
