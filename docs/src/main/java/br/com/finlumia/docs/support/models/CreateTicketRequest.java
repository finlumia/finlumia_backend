package br.com.finlumia.docs.support.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(min = 5, max = 255)
        String title,

        @NotNull
        @Pattern(regexp = "duvida|bug|melhoria|acesso|outros",
                 message = "category deve ser: duvida, bug, melhoria, acesso ou outros")
        String category,

        @Pattern(regexp = "baixa|media|alta|urgente",
                 message = "priority deve ser: baixa, media, alta ou urgente")
        String priority,

        @NotBlank @Size(min = 20)
        String description
) {
}
