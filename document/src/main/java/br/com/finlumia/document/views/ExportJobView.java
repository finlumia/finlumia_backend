package br.com.finlumia.document.views;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExportJobView(
        ExportJobStatus status,
        String downloadUrl,
        @JsonProperty("expiresAt") Instant expiresAt
) {
}
