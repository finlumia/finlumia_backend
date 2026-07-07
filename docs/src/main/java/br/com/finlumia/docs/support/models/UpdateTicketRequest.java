package br.com.finlumia.docs.support.models;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Pattern(regexp = "aberto|em_analise|respondido|fechado",
                 message = "status deve ser: aberto, em_analise, respondido ou fechado")
        String status,

        @Pattern(regexp = "baixa|media|alta|urgente",
                 message = "priority deve ser: baixa, media, alta ou urgente")
        String priority,

        @Size(max = 100)
        String assignedTo
) {
}
