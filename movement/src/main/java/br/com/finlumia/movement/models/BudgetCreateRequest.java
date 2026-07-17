package br.com.finlumia.movement.models;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BudgetCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull TransactionType type,
        @NotNull BudgetScope scope,
        @JsonProperty("scopeValue") String scopeValue,
        @NotNull @DecimalMin("0.01") @JsonProperty("limitAmount") BigDecimal limitAmount,
        @NotNull @JsonProperty("periodStart") LocalDate periodStart,
        @NotNull @JsonProperty("periodEnd") LocalDate periodEnd
) {}
