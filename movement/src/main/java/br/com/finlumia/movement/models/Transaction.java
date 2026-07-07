package br.com.finlumia.movement.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record Transaction(
        UUID id,
        UUID userKey,
        TransactionType type,
        PaymentMethod method,
        InstitutionId institution,
        LocalDate date,
        CategoryId category,
        String description,
        String subDescription,
        BigDecimal amount,
        String notes,
        List<String> tags,
        boolean isRecurring,
        UUID recurringId,
        Instant createdAt,
        Instant updatedAt
) {}
