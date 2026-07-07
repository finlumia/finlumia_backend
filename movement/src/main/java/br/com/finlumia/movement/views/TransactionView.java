package br.com.finlumia.movement.views;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.com.finlumia.movement.models.CategoryId;
import br.com.finlumia.movement.models.InstitutionId;
import br.com.finlumia.movement.models.PaymentMethod;
import br.com.finlumia.movement.models.Transaction;
import br.com.finlumia.movement.models.TransactionType;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TransactionView(
        UUID id,
        TransactionType type,
        PaymentMethod method,
        InstitutionId institution,
        LocalDate date,
        CategoryId category,
        String description,
        @JsonProperty("subDescription") String subDescription,
        BigDecimal amount,
        String notes,
        List<String> tags,
        @JsonProperty("isRecurring") boolean isRecurring,
        @JsonProperty("recurringId") UUID recurringId,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("updatedAt") Instant updatedAt
) {
    public static TransactionView from(Transaction t) {
        return new TransactionView(
                t.id(), t.type(), t.method(), t.institution(), t.date(),
                t.category(), t.description(), t.subDescription(), t.amount(),
                t.notes(), t.tags(), t.isRecurring(), t.recurringId(),
                t.createdAt(), t.updatedAt());
    }
}
