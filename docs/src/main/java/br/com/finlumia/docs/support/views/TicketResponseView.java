package br.com.finlumia.docs.support.views;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record TicketResponseView(
        UUID id,
        AuthorRef author,
        String message,
        @JsonProperty("is_internal") boolean isInternal,
        @JsonProperty("created_at") Instant createdAt
) {
}
