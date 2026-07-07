package br.com.finlumia.movement.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionFilters(
        int page,
        int pageSize,
        TransactionType type,
        PaymentMethod method,
        InstitutionId institution,
        CategoryId category,
        LocalDate dateStart,
        LocalDate dateEnd,
        BigDecimal amountMin,
        BigDecimal amountMax,
        String search,
        SortBy sortBy,
        SortOrder sortOrder
) {
    public TransactionFilters(
            Integer page, Integer pageSize,
            TransactionType type, PaymentMethod method, InstitutionId institution,
            CategoryId category, LocalDate dateStart, LocalDate dateEnd,
            BigDecimal amountMin, BigDecimal amountMax, String search,
            SortBy sortBy, SortOrder sortOrder) {
        this(
                (page == null || page < 1) ? 1 : page,
                (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100),
                type, method, institution, category, dateStart, dateEnd,
                amountMin, amountMax, search,
                sortBy != null ? sortBy : SortBy.DATE,
                sortOrder != null ? sortOrder : SortOrder.DESC
        );
    }
}
