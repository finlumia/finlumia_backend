package br.com.finlumia.configurator.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFieldRequest(
        @NotBlank @Size(max = 63) String name,
        @NotNull UUID tableId,
        @NotNull DataTypeEnum dataType,
        Integer length,
        Boolean nullable,
        String defaultValue,
        Boolean isPrimary,
        Boolean isForeign,
        String referencesTable,
        String referencesField,
        StatusEnum status) {
}
