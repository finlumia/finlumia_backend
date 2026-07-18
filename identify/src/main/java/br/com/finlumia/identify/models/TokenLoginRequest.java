package br.com.finlumia.identify.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TokenLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        Boolean remember) {
}
