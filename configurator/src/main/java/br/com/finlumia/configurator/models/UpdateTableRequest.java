package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
@Schema(description = "Payload para atualizacao de tabela no configurador")
public class UpdateTableRequest {

    @JsonProperty("keyTable")
    @NotNull(message = "keyTable is required")
    @Positive(message = "keyTable must be greater than zero")
    @Schema(
            description = "Chave identificadora da tabela",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long keyTable;

    @JsonProperty("schemaName")
    @Size(max = 63, message = "schemaName must have at most 63 characters")
    @Schema(
            description = "Nome do schema no PostgreSQL",
            example = "configurator",
            maxLength = 63)
    private String schemaName;

    @JsonProperty("tableName")
    @Size(max = 63, message = "tableName must have at most 63 characters")
    @Schema(
            description = "Nome fisico da tabela no PostgreSQL",
            example = "tables",
            maxLength = 63)
    private String tableName;

    @JsonProperty("displayName")
    @NotBlank(message = "displayName is required")
    @Size(max = 120, message = "displayName must have at most 120 characters")
    @Schema(
            description = "Nome amigavel exibido na interface",
            example = "Tabelas",
            maxLength = 120,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String displayName;
    
    @JsonProperty("description")
    @Size(max = 500, message = "description must have at most 500 characters")
    @Schema(
            description = "Descricao opcional da tabela",
            example = "Tabela de metadados do configurador",
            maxLength = 500)
    private String description;
    
}
