package br.com.finlumia.identify.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccessControlSelfCheckRequest(
        @NotBlank String resourceName,
        @NotNull AccessOperation operation) {
}
