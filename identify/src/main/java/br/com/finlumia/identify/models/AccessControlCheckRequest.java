package br.com.finlumia.identify.models;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccessControlCheckRequest(
        @NotNull UUID userKey,
        @NotBlank String resourceName,
        @NotNull AccessOperation operation) {
}
