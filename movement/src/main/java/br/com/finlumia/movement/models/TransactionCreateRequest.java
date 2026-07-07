package br.com.finlumia.movement.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransactionCreateRequest(
        @NotNull TransactionType type,
        @NotNull PaymentMethod method,
        @NotNull InstitutionId institution,
        @NotNull LocalDate date,
        @NotNull CategoryId category,
        @NotBlank @Size(max = 500) String description,
        @JsonProperty("subDescription") @Size(max = 255) String subDescription,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String notes,
        List<String> tags,
        @JsonProperty("isRecurring") Boolean isRecurring,
        @JsonProperty("recurringMonths") @Min(1) @Max(36) Integer recurringMonths
) {}
