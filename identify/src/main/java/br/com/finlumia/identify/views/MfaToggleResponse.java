package br.com.finlumia.identify.views;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MfaToggleResponse(
        boolean mfa,
        @JsonProperty("qr_code_url") String qrCodeUrl) {
}
