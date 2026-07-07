package br.com.finlumia.document.views;

import java.util.List;

public record CashFlowView(
        List<MonthlySummaryView> data,
        KpiSummaryView kpis
) {
}
