package br.com.finlumia.identify.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String resetSession,
        @NotBlank @Size(min = 8) String newPassword,
        @NotBlank @Size(min = 8) String confirmPassword) {
}
