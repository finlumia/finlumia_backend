package br.com.finlumia.identify.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank @JsonProperty("id_token") String idToken) {
}
