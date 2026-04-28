package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenericListRequest {
    
    @JsonProperty("slugTable")
    @NotBlank(message = "slugTable is required")
    @Size(max = 100, message = "slugTable must have at most 63 characters")
    @Schema(
            description = "Slug da tabela",
            example = "table_example",
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String slugTable;

    @JsonProperty("page")
    @NotNull(message = "page is required")
    @Min(value = 1, message = "page must be greater than or equal to 1")
    @Schema(
            description = "Pagina",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer page;
    
}
