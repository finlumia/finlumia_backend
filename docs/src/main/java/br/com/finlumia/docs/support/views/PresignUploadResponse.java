package br.com.finlumia.docs.support.views;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record PresignUploadResponse(
        @JsonProperty("attachment_id") UUID attachmentId,
        @JsonProperty("upload_url") String uploadUrl,
        @JsonProperty("expires_at") Instant expiresAt
) {
}
