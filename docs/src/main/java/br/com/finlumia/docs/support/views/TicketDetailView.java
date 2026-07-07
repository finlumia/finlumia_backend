package br.com.finlumia.docs.support.views;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketDetailView(
        UUID id,
        @JsonProperty("ticket_code") String ticketCode,
        UserRef user,
        String title,
        String category,
        String priority,
        String status,
        String description,
        @JsonProperty("assigned_to") String assignedTo,
        List<TicketResponseView> responses,
        List<TicketAttachmentView> attachments,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
}
