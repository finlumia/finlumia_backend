package br.com.finlumia.identify.views;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ForgotPasswordResponse(
        String message,
        @JsonProperty("expires_in_seconds") long expiresInSeconds) {
}
