package br.com.finlumia.identify.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public record TokenRevokeRequest(
        @NotBlank @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken) {
}
