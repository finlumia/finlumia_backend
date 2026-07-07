package br.com.finlumia.identify.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ToggleMfaRequest(
        @NotNull Boolean enabled,
        @NotBlank @JsonProperty("current_password") String currentPassword) {
}
