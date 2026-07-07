package br.com.finlumia.movement.views;

import java.util.List;

public record TransactionListView(
        List<TransactionView> data,
        PaginationMeta meta,
        TotalsView totals
) {}
