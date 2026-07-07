package br.com.finlumia.identify.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @JsonProperty("reset_session") String resetSession,
        @NotBlank @Size(min = 8) @JsonProperty("new_password") String newPassword,
        @NotBlank @Size(min = 8) @JsonProperty("confirm_password") String confirmPassword) {
}
