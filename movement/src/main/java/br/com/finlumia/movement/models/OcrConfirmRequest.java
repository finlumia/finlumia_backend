package br.com.finlumia.movement.models;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OcrConfirmRequest(
        @NotNull TransactionType type,
        @NotBlank String description,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate date,
        @NotNull CategoryId category,
        @NotNull PaymentMethod method,
        InstitutionId institution
) {}
