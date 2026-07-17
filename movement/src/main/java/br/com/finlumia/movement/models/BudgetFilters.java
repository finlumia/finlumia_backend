package br.com.finlumia.movement.models;

public record BudgetFilters(
        int page,
        int pageSize,
        TransactionType type
) {
    public BudgetFilters(Integer page, Integer pageSize, TransactionType type) {
        this(
                (page == null || page < 1) ? 1 : page,
                (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100),
                type
        );
    }
}
