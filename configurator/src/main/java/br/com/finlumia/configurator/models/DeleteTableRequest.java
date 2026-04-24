package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

@Data
@Schema(description = "Payload para exclusao logica de tabela no configurador")
public class DeleteTableRequest {
    
    @JsonProperty("keyTable")
    @NotNull(message = "keyTable is required")
    @Positive(message = "keyTable must be greater than zero")
    @Schema(
            description = "Chave identificadora da tabela",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long keyTable;

}
