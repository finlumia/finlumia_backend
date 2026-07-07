package br.com.finlumia.docs.support.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddResponseRequest(
        @NotBlank
        String message,

        Boolean isInternal,

        @Pattern(regexp = "aberto|em_analise|respondido|fechado",
                 message = "newStatus deve ser: aberto, em_analise, respondido ou fechado")
        String newStatus
) {
}
