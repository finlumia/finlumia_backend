package br.com.finlumia.docs.support.views;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record TicketListItem(
        UUID id,
        @JsonProperty("ticket_code") String ticketCode,
        UserRef user,
        String title,
        String category,
        String priority,
        String status,
        String description,
        @JsonProperty("assigned_to") String assignedTo,
        @JsonProperty("response_count") long responseCount,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
