package br.com.finlumia.movement.views;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import br.com.finlumia.movement.models.Budget;
import br.com.finlumia.movement.models.BudgetScope;
import br.com.finlumia.movement.models.TransactionType;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BudgetView(
        UUID id,
        String name,
        TransactionType type,
        BudgetScope scope,
        @JsonProperty("scopeValue") String scopeValue,
        @JsonProperty("limitAmount") BigDecimal limitAmount,
        @JsonProperty("periodStart") LocalDate periodStart,
        @JsonProperty("periodEnd") LocalDate periodEnd,
        @JsonProperty("currentTotal") BigDecimal currentTotal,
        @JsonProperty("progressPercent") BigDecimal progressPercent,
        @JsonProperty("notifiedAt") Instant notifiedAt,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("updatedAt") Instant updatedAt
) {
    public static BudgetView from(Budget b, BigDecimal currentTotal) {
        BigDecimal percent = b.limitAmount().signum() == 0
                ? BigDecimal.ZERO
                : currentTotal.divide(b.limitAmount(), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
        return new BudgetView(
                b.id(), b.name(), b.type(), b.scope(), b.scopeValue(),
                b.limitAmount(), b.periodStart(), b.periodEnd(),
                currentTotal, percent, b.notifiedAt(), b.createdAt(), b.updatedAt());
    }
}
