package br.com.finlumia.movement.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Budget(
        UUID id,
        UUID userKey,
        String name,
        TransactionType type,
        BudgetScope scope,
        String scopeValue,
        BigDecimal limitAmount,
        LocalDate periodStart,
        LocalDate periodEnd,
        Instant notifiedAt,
        Instant createdAt,
        Instant updatedAt
) {}
