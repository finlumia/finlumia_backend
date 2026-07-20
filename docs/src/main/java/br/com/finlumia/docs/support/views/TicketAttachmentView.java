package br.com.finlumia.docs.support.views;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record TicketAttachmentView(
        UUID id,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("file_size_bytes") int fileSizeBytes,
        @JsonProperty("mime_type") String mimeType,
        String url,
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        @JsonProperty("conversion_status") String conversionStatus,
        @JsonProperty("created_at") Instant createdAt
) {
}
