package br.com.finlumia.movement.views;

import java.util.List;

public record BudgetListView(List<BudgetView> data, PaginationMeta meta) {}
