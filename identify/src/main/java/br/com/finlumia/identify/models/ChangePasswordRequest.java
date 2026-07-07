package br.com.finlumia.identify.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @JsonProperty("current_password") String currentPassword,
        @NotBlank @Size(min = 8) @JsonProperty("new_password") String newPassword,
        @NotBlank @JsonProperty("confirm_password") String confirmPassword) {
}
