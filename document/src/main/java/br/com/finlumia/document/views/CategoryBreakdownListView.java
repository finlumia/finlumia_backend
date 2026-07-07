package br.com.finlumia.document.views;

import java.math.BigDecimal;
import java.util.List;

public record CategoryBreakdownListView(
        List<CategoryBreakdownView> data,
        BigDecimal total
) {
}
