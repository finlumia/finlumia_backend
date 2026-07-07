package br.com.finlumia.configurator.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFunctionRequest(
        @NotBlank @Size(max = 63) String name,
        @NotNull SchemaEnum schema,
        @NotNull LanguageEnum language,
        @NotBlank String returnType,
        String args,
        @NotNull VolatilityEnum volatility,
        @NotBlank String body,
        String description,
        StatusEnum status) {
}
