package br.com.finlumia.identify.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 255) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password) {
}
