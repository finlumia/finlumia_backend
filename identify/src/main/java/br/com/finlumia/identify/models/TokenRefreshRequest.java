package br.com.finlumia.identify.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @NotBlank @JsonProperty("refresh_token") String refreshToken) {
}
