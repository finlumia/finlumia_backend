package br.com.finlumia.identify.views;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VerifyResetTokenResponse(
        @JsonProperty("reset_session") String resetSession) {
}
