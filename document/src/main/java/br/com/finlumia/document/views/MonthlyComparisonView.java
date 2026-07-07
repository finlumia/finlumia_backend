package br.com.finlumia.document.views;

import java.util.List;

public record MonthlyComparisonView(
        List<MonthlyComparisonItemView> data
) {
}
